import re
from validation.utils.structures import ColComb, UCC, IND


DATASET = "dataset_results/TPCH_12"
UCC_FILE = f"{DATASET}/ucc.txt"
IND_FILE = f"{DATASET}/ind.txt"

QUERY_SPECS = [
    ("T6", "rhs", "lhs", "projected"),
    ("T7", "rhs", "rhs", "projected"),
    ("T8", "rhs", "lhs", "ucc"),
    ("T9", "rhs", "rhs", "ucc"),
    ("T10", "lhs", "lhs", "ucc"),
    ("T11", "lhs", "rhs", "ucc"),
    ("T12", "lhs", "lhs", "projected"),
    ("T13", "lhs", "rhs", "projected")
]


def read_lines(path: str) -> list[str]:
    with open(path, encoding='utf-8-sig') as f:
        return [line.strip() for line in f.read().splitlines() if line.strip()]

def parse_group_ordered(bracket_content: str) -> tuple:
    if not bracket_content.strip():
        return tuple()
    return tuple(item.strip() for item in bracket_content.split(',') if item.strip())

def parse_group(bracket_content: str) -> ColComb:
    if not bracket_content.strip():
        return frozenset()
    return frozenset(item.strip() for item in bracket_content.split(',') if item.strip())

def load_uccs(path: str) -> list[UCC]:
    result = []
    for line in read_lines(path):
        groups = [parse_group(m) for m in re.findall(r'\[([^\]]*)\]', line)]
        if len(groups) == 1:
            result.append(UCC(lhs=groups[0]))
        else:
            print(f"[WARN] Skipping malformed UCC line: {line}")
    return result

def load_inds(path: str) -> list[IND]:
    result = []
    for line in read_lines(path):
        groups = [parse_group_ordered(m) for m in re.findall(r'\[([^\]]*)\]', line)]
        if len(groups) == 2:
            result.append(IND(lhs=groups[0], rhs=groups[1]))
        else:
            print(f"[WARN] Skipping malformed IND line: {line}")
    return result

# ── FORMATTING ────────────────────────────────────────────────────────────────

def fmt(col_comb) -> str:
    if isinstance(col_comb, (frozenset, set)):
        return '[' + ', '.join(sorted(col_comb)) + ']'
    return '[' + ', '.join(col_comb) + ']'

# ── CORE ──────────────────────────────────────────────────────────────────────

def project(ind: IND, target_attrs: list, match_side: str) -> frozenset | None:

    side      = list(ind.rhs) if match_side == 'rhs' else list(ind.lhs)
    opp_side  = list(ind.lhs) if match_side == 'rhs' else list(ind.rhs)

    if not all(attr in side for attr in target_attrs):
        return None

    return frozenset(opp_side[side.index(attr)] for attr in target_attrs)


def get_ind2_side(ind2: IND, projected_x: frozenset, match_side: str) -> frozenset | None:

    side     = frozenset(ind2.lhs) if match_side == 'lhs' else frozenset(ind2.rhs)
    opp_side = frozenset(ind2.rhs) if match_side == 'lhs' else frozenset(ind2.lhs)

    if len(side) != len(projected_x):
        return None
    if side != projected_x:
        return None

    return opp_side


def run_query(inds: list[IND], uccs: list[UCC], ucc_on: str, ind2_match: str, ind2_target: str) -> list[tuple]:
    seen = set()
    all_matches = []

    for ucc in uccs:
        ucc_attrs = list(ucc.lhs)

        for ind1 in inds:
            # Project opposite side of ucc_on
            projected = project(ind1, ucc_attrs, match_side=ucc_on)
            if projected is None:
                continue

            key = (ucc.lhs, projected)
            if key in seen:
                continue
            seen.add(key)

            # Decide which variable to match IND2 against
            match_target = ucc.lhs if ind2_target == "ucc" else projected

            for ind2 in inds:
                z = get_ind2_side(ind2, match_target, match_side=ind2_match)
                if z is None:
                    continue

                all_matches.append((
                    frozenset(ind2.lhs),
                    frozenset(ind2.rhs),
                    projected,
                    frozenset(ind1.lhs),
                    frozenset(ind1.rhs),
                    ucc.lhs
                ))

    return all_matches

def run_all_queries():
    inds = load_inds(IND_FILE)
    uccs = load_uccs(UCC_FILE)
    total_all = 0

    for label, ucc_on, ind2_match, use_y in QUERY_SPECS:
        print(f"\n{'='*60}")
        print(f"  {label}")
        print(f"{'='*60}")

        matches = run_query(inds, uccs, ucc_on, ind2_match, use_y)

        # for ind2_lhs, ind2_rhs, x, ind1_lhs, ind1_rhs, ucc_lhs in matches:
        #     print(f"  {fmt(ind2_lhs)}, {fmt(ind2_rhs)}, {fmt(x)}, {fmt(ind1_lhs)}, {fmt(ind1_rhs)}, {fmt(ucc_lhs)}")

        print(f"  Matches: {len(matches)}")
        total_all += len(matches)

    print(f"\n{'='*60}")
    print(f"  Total across all queries: {total_all}")
    print(f"{'='*60}")


# ── MAIN ──────────────────────────────────────────────────────────────────────

if __name__ == "__main__":
    run_all_queries()