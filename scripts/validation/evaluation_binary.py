import re
from validation.utils.structures import ColComb, UCC, FD, IND


# ── PARSING ───────────────────────────────────────────────────────────────────
def load_inds(path: str) -> list[IND]:
    result = []
    for line in read_lines(path):
        groups = parse_groups_ordered(line)  # ← ordered, not frozenset
        if len(groups) == 2:
            result.append(IND(lhs=groups[0], rhs=groups[1]))  # already tuples
        else:
            print(f"[WARN] Skipping malformed IND line: {line}")
    return result

def parse_groups_ordered(line: str) -> list[tuple]:
    return [parse_group_ordered(m) for m in re.findall(r'\[([^\]]*)\]', line)]

def parse_group_ordered(bracket_content: str) -> tuple:
    if not bracket_content.strip():
        return tuple()
    return tuple(item.strip() for item in bracket_content.split(','))

def parse_groups(line: str) -> list[ColComb]:
    return [parse_group(m) for m in re.findall(r'\[([^\]]*)\]', line)]

def parse_group(bracket_content: str) -> ColComb:
    if not bracket_content.strip():
        return frozenset()
    return frozenset(item.strip() for item in bracket_content.split(','))

def read_lines(path: str) -> list[str]:
    return [l.strip() for l in open(path).read().splitlines() if l.strip()]

def load_uccs(path: str) -> list[UCC]:
    result = []
    for line in read_lines(path):
        groups = parse_groups(line)
        if len(groups) == 1:
            result.append(UCC(lhs=groups[0]))
        else:
            print(f"[WARN] Skipping malformed UCC line: {line}")
    return result

def load_fds(path: str) -> list[FD]:
    result = []
    for line in read_lines(path):
        groups = parse_groups(line)
        if len(groups) == 2:
            result.append(FD(lhs=groups[0], rhs=groups[1]))
        else:
            print(f"[WARN] Skipping malformed FD line: {line}")
    return result

def get_table(col: str) -> str:
    return col.split('.')[0]

def get_table_from_comb(comb: ColComb) -> str | None:
    # Returns table name if all cols are from same table, else None
    tables = {get_table(col) for col in comb}
    return tables.pop() if len(tables) == 1 else None

# ── FORMATTING ────────────────────────────────────────────────────────────────

def fmt(col_comb: ColComb) -> str:
    return '[' + ', '.join(sorted(col_comb)) + ']'

def fmt_ind(ind: IND) -> str:
    return f"{fmt(ind.lhs)}, {fmt(ind.rhs)}"

def fmt_fd(fd: FD) -> str:
    return f"{fmt(fd.lhs)}, {fmt(fd.rhs)}"

def print_header(query: str):
    print(f"\n{'='*60}")
    print(f"  QUERY: {query}")
    print(f"{'='*60}")

# ── QUERY METHODS ─────────────────────────────────────────────────────────────
def b1():
    fds  = load_fds(FD_FILE)
    uccs = load_uccs(UCC_FILE)
    all_matches = []
    count = 0

    with open("../backup/dataset_results/TPCH_12/b1_test.txt", "w", encoding="utf-8") as out:
        for ucc in uccs:
            minimal_lhs_list = find_minimal_lhs_for_rhs_union(fds, ucc.lhs)
            if minimal_lhs_list:
                for lhs in minimal_lhs_list:
                    line = f"{fmt(lhs)}, {fmt(ucc.lhs)}"
                    print(line)
                    out.write(line + "\n")
                    all_matches.append((lhs, ucc.lhs))
                count += len(minimal_lhs_list)

    print(f"Total Count: {count}")
    return all_matches

def b2():
    fds  = load_fds(FD_FILE)
    uccs = load_uccs(UCC_FILE)
    all_matches = []

    for ucc in uccs:
        ucc_table = get_table_from_comb(ucc.lhs)

        matching_fds = []
        for fd in fds:
            if fd.lhs == frozenset():
                # Empty LHS — match only if RHS table matches UCC table
                fd_table = get_table_from_comb(fd.rhs)
                if fd_table == ucc_table:
                    matching_fds.append(fd)
            else:
                # Normal subset check
                if fd.lhs <= ucc.lhs:
                    matching_fds.append(fd)

        if not matching_fds:
            continue

        # Deduplicate by rhs
        seen_rhs = set()
        deduped = []
        for fd in matching_fds:
            if fd.rhs not in seen_rhs:
                seen_rhs.add(fd.rhs)
                deduped.append(fd)

        for fd in deduped:
            print(f"{fmt(ucc.lhs)} -> {fmt(fd.rhs)}")
            all_matches.append((ucc, fd))

    return all_matches

def b3():
    inds = load_inds(IND_FILE)
    uccs = load_uccs(UCC_FILE)
    seen = set()
    all_matches = []

    for ucc in uccs:
        ucc_attrs = list(ucc.lhs)

        for ind in inds:
            ind_rhs = list(ind.rhs)  # ordered
            ind_lhs = list(ind.lhs)  # ordered

            # IND rhs must contain ALL ucc_attrs
            if not all(attr in ind_rhs for attr in ucc_attrs):
                continue

            # Project ind_lhs at positions where ucc attrs appear in ind_rhs
            projected_lhs = frozenset(ind_lhs[ind_rhs.index(attr)] for attr in ucc_attrs)

            key = (ucc.lhs, projected_lhs)
            if key in seen:
                continue
            seen.add(key)

            all_matches.append((ucc, projected_lhs))
            print(f"{fmt(ucc.lhs)}")

    return all_matches

def b4():
    inds = load_inds(IND_FILE)
    uccs = load_uccs(UCC_FILE)
    seen = set()
    all_matches = []

    for ucc in uccs:
        ucc_attrs = list(ucc.lhs)

        for ind in inds:
            ind_rhs = list(ind.rhs)  # ordered
            ind_lhs = list(ind.lhs)  # ordered

            # IND lhs must contain ALL ucc_attrs
            if not all(attr in ind_lhs for attr in ucc_attrs):
                continue

            # Project ind_rhs at positions where ucc attrs appear in ind_lhs
            projected_rhs = frozenset(ind_rhs[ind_lhs.index(attr)] for attr in ucc_attrs)

            key = (ucc.lhs, projected_rhs)
            if key in seen:
                continue
            seen.add(key)

            all_matches.append((ucc, projected_rhs))
            print(f"{fmt(ucc.lhs)}, {fmt(projected_rhs)}")

    return all_matches

def b5():
    inds = load_inds(IND_FILE)
    fds  = load_fds(FD_FILE)
    seen = set()
    all_matches = []

    for fd in fds:
        fd_lhs_attrs = list(fd.lhs)

        for ind in inds:
            ind_rhs = list(ind.rhs)  # ordered
            ind_lhs = list(ind.lhs)  # ordered

            # IND rhs must contain ALL fd_lhs attrs
            if not all(attr in ind_rhs for attr in fd_lhs_attrs):
                continue

            # Project ind_lhs at positions where fd_lhs attrs appear in ind_rhs
            projected_lhs = frozenset(ind_lhs[ind_rhs.index(attr)] for attr in fd_lhs_attrs)

            key = (fd.lhs, fd.rhs, projected_lhs)
            if key in seen:
                continue
            seen.add(key)

            all_matches.append((fd, projected_lhs))
            print(f"{fmt(projected_lhs)}, {fmt(fd.lhs)}, {fmt(fd.rhs)}")

    return all_matches

def b6():
    inds = load_inds(IND_FILE)
    fds  = load_fds(FD_FILE)
    seen = set()
    all_matches = []

    for fd in fds:
        fd_lhs_attrs = list(fd.lhs)

        for ind in inds:
            ind_rhs = list(ind.rhs)  # ordered
            ind_lhs = list(ind.lhs)  # ordered

            # IND lhs must contain ALL fd_lhs attrs
            if not all(attr in ind_lhs for attr in fd_lhs_attrs):
                continue

            # Project ind_rhs at positions where fd_lhs attrs appear in ind_lhs
            projected_rhs = frozenset(ind_rhs[ind_lhs.index(attr)] for attr in fd_lhs_attrs)

            key = (fd.lhs, fd.rhs, projected_rhs)
            if key in seen:
                continue
            seen.add(key)

            all_matches.append((fd, projected_rhs))
            print(f"{fmt(fd.lhs)}, {fmt(projected_rhs)}, {fmt(fd.rhs)}")

    return all_matches

def b7():
    inds = load_inds(IND_FILE)
    fds  = load_fds(FD_FILE)
    seen = set()
    all_matches = []

    for fd in fds:
        fd_rhs_attrs = list(fd.rhs)  # fd.rhs is frozenset, order doesn't matter here

        for ind in inds:
            ind_rhs = list(ind.rhs)  # ordered
            ind_lhs = list(ind.lhs)  # ordered

            # Check all fd_rhs attrs exist in ind_rhs
            if not all(attr in ind_rhs for attr in fd_rhs_attrs):
                continue

            # Project ind_lhs at positions where fd_rhs attrs appear in ind_rhs
            projected_lhs = frozenset(ind_lhs[ind_rhs.index(attr)] for attr in fd_rhs_attrs)

            key = (fd.lhs, fd.rhs, projected_lhs)
            if key in seen:
                continue
            seen.add(key)

            all_matches.append((fd, projected_lhs))
            print(f"{fmt(projected_lhs)} ⊆ {fmt(fd.rhs)} \t\t AND {fmt(fd.lhs)} -> {fmt(fd.rhs)}")

    return all_matches

def b8():
    inds = load_inds(IND_FILE)
    fds  = load_fds(FD_FILE)
    seen = set()
    all_matches = []

    for fd in fds:
        fd_rhs_attrs = list(fd.rhs)

        for ind in inds:
            ind_rhs = list(ind.rhs)  # ordered
            ind_lhs = list(ind.lhs)  # ordered

            # Check all fd_rhs attrs exist in ind_lhs (matching against IND lhs side)
            if not all(attr in ind_lhs for attr in fd_rhs_attrs):
                continue

            # Project ind_rhs at positions where fd_rhs attrs appear in ind_lhs
            projected_rhs = frozenset(ind_rhs[ind_lhs.index(attr)] for attr in fd_rhs_attrs)

            key = (fd.lhs, fd.rhs, projected_rhs)
            if key in seen:
                continue
            seen.add(key)

            all_matches.append((fd, projected_rhs))
            print(f"{fmt(fd.rhs)}, {fmt(projected_rhs)}, {fmt(fd.lhs)}")

    return all_matches

def prune(candidates: list[ColComb]) -> list[ColComb]:
    """Assumes sorted by size ascending. Keeps only minimals."""
    minimal = []
    for c in candidates:
        if not any(m < c for m in minimal):
            minimal.append(c)
    return minimal

def find_minimal_lhs_for_rhs_union(fds: list[FD], target: ColComb) -> list[ColComb]:
    target_attrs = list(target)

    # Build rhs index
    rhs_index: dict[str, list[ColComb]] = {}
    for fd in fds:
        if len(fd.rhs) == 1:
            attr = next(iter(fd.rhs))
            rhs_index.setdefault(attr, []).append(fd.lhs)

    # Build groups with disjoint check
    groups = []
    for attr in target_attrs:
        covering = [lhs for lhs in rhs_index.get(attr, []) if lhs.isdisjoint(target)]
        if not covering:
            return []
        groups.append(covering)

    # Start with first group
    minimal = list(dict.fromkeys(groups[0]))
    minimal.sort(key=len)
    minimal = prune(minimal)

    # Iteratively merge with each next group
    for group in groups[1:]:
        merged = []
        for existing in minimal:
            for lhs in group:
                merged.append(existing | lhs)

        merged = list(dict.fromkeys(merged))
        merged.sort(key=len)
        minimal = prune(merged)

    return minimal

QUERY = "b2"

# Only set the files your query actually needs (others can be left as None)
DATASET = "dataset_results/WDC"
FD_FILE  = f"{DATASET}/fd.txt"
UCC_FILE = f"{DATASET}/ucc.txt"
IND_FILE = f"{DATASET}/ind.txt"

QUERIES = {
    "b1": b1,
    "b2": b2,
    "b3": b3,
    "b4": b4,
    "b5": b5,
    "b6": b6,
    "b7": b7,
    "b8": b8
}

def run_query(query: str):
    print_header(query)
    fn = QUERIES.get(query)
    if fn is None:
        print(f"[ERROR] Unknown query: '{query}'")
        print("Valid queries:")
        for k in QUERIES:
            print(f"  {k}")
        return
    matches = fn()
    print(f"\n  Total matches: {len(matches)}")

# ── MAIN ──────────────────────────────────────────────────────────────────────

if __name__ == "__main__":
    run_query(QUERY)