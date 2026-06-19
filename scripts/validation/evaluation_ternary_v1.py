import re
from dataclasses import dataclass
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
def t1():
    inds = load_inds(IND_FILE)
    uccs = load_uccs(UCC_FILE)

    ucc_lhs_list = [u.lhs for u in uccs]
    seen_ind = set()
    all_matches = []

    for ucc_y in uccs:
        ucc_attrs = list(ucc_y.lhs)

        for ind in inds:
            ind_rhs = list(ind.rhs)  # ordered
            ind_lhs = list(ind.lhs)  # ordered

            # IND rhs must contain ALL ucc_y attrs
            if not all(attr in ind_rhs for attr in ucc_attrs):
                continue

            # Project both sides
            positions = [ind_rhs.index(attr) for attr in ucc_attrs]
            projected_lhs = frozenset(ind_lhs[p] for p in positions)  # X
            projected_rhs = frozenset(ind_rhs[p] for p in positions)  # Y == ucc_y.lhs

            key = (ucc_y.lhs, projected_lhs)
            if key in seen_ind:
                continue
            seen_ind.add(key)

            # Check if any UCC is subset of or equal to projected_lhs (X)
            matching_ucc_x = [u for u in ucc_lhs_list if u <= projected_lhs]
            if not matching_ucc_x:
                continue

            for ucc_x in matching_ucc_x:
                print(f"{fmt(projected_lhs)}, {fmt(projected_rhs)}")
                all_matches.append((ucc_x, projected_lhs, projected_rhs, ucc_y.lhs))

    return all_matches

def t3():
    fds  = load_fds(FD_FILE)
    inds = load_inds(IND_FILE)

    # Keep only unary INDs
    unary_inds = [ind for ind in inds if len(ind.lhs) == 1 and len(ind.rhs) == 1]

    # Build IND index: (lhs_attr, rhs_attr) → True
    ind_index = {(next(iter(frozenset(ind.lhs))), next(iter(frozenset(ind.rhs)))) for ind in unary_inds}

    seen = set()
    all_matches = []

    print("\n=== t3: FD(W,X) AND IND(X,Y) AND FD(Z,Y) ===")

    for fd1 in fds:
        if len(fd1.rhs) != 1:
            continue
        x = next(iter(fd1.rhs))

        for fd2 in fds:
            if len(fd2.rhs) != 1:
                continue
            y = next(iter(fd2.rhs))

            if x == y:
                continue

            # Check if IND(X,Y) exists
            if (x, y) not in ind_index:
                continue

            key = (fd1.lhs, fd1.rhs, fd2.lhs, fd2.rhs)
            if key in seen:
                continue
            seen.add(key)

            print(f"{fmt(fd1.lhs)}, {fmt(fd1.rhs)}, {fmt(fd2.rhs)}, {fmt(fd2.lhs)}")
            all_matches.append((fd1.lhs, fd1.rhs, fd2.lhs, fd2.rhs))

    return all_matches

def t4():
    fds  = load_fds(FD_FILE)
    inds = load_inds(IND_FILE)

    seen = set()
    all_matches = []

    print("\n=== t4: FD(X,W) AND IND(X,Y) AND FD(Z,Y) ===")

    for ind in inds:
        ind_lhs = list(ind.lhs)
        ind_rhs = list(ind.rhs)

        # FD1 lhs subset of IND lhs (X), rhs disjoint from IND lhs
        for fd1 in fds:
            if not fd1.lhs <= frozenset(ind_lhs):
                continue
            if not fd1.rhs.isdisjoint(frozenset(ind_lhs)):
                continue

            # Project IND rhs at positions of fd1.lhs attrs in IND lhs → Y
            fd1_lhs_attrs = list(fd1.lhs)
            if not all(attr in ind_lhs for attr in fd1_lhs_attrs):
                continue

            positions = [ind_lhs.index(attr) for attr in fd1_lhs_attrs]
            projected_y = frozenset(ind_rhs[p] for p in positions)

            # Find minimal LHS covering projected_y (Y) via FD2 rhs → Z
            z_list = find_minimal_lhs_for_rhs_union(fds, projected_y)
            if not z_list:
                continue

            for z in z_list:
                key = (fd1.lhs, fd1.rhs, projected_y, z)
                if key in seen:
                    continue
                seen.add(key)

                print(f"{fmt(fd1.lhs)}, {fmt(fd1.rhs)}, {fmt(projected_y)}, {fmt(z)}")
                all_matches.append((fd1.lhs, fd1.rhs, projected_y, z))

    return all_matches

def t5():
    fds  = load_fds(FD_FILE)
    inds = load_inds(IND_FILE)

    seen = set()
    all_matches = []

    print("\n=== t5: FD(W,X) AND IND(X,Y) AND FD(Y,Z) ===")

    for ind in inds:
        ind_lhs = list(ind.lhs)
        ind_rhs = list(ind.rhs)

        # FD2 lhs subset of IND rhs (Y), rhs disjoint from IND rhs
        for fd2 in fds:
            if not fd2.lhs <= frozenset(ind_rhs):
                continue
            if not fd2.rhs.isdisjoint(frozenset(ind_rhs)):
                continue

            # Project IND lhs at positions of fd2.lhs attrs in IND rhs → X
            fd2_lhs_attrs = list(fd2.lhs)
            if not all(attr in ind_rhs for attr in fd2_lhs_attrs):
                continue

            positions = [ind_rhs.index(attr) for attr in fd2_lhs_attrs]
            projected_x = frozenset(ind_lhs[p] for p in positions)

            # Find minimal LHS covering projected_x (X) via FD1 rhs → W
            w_list = find_minimal_lhs_for_rhs_union(fds, projected_x)
            if not w_list:
                continue

            for w in w_list:
                key = (w, projected_x, fd2.lhs, fd2.rhs)
                if key in seen:
                    continue
                seen.add(key)

                print(f"{fmt(w)}, {fmt(projected_x)}, {fmt(fd2.lhs)}, {fmt(fd2.rhs)}")
                all_matches.append((w, projected_x, fd2.lhs, fd2.rhs))

    return all_matches

def t14():
    inds = load_inds(IND_FILE)
    uccs = load_uccs(UCC_FILE)
    fds  = load_fds(FD_FILE)

    seen_ind = set()
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
            if key in seen_ind:
                continue
            seen_ind.add(key)

            # projected_lhs acts as UCC lhs for second method
            virtual_ucc = projected_lhs
            virtual_ucc_table = get_table_from_comb(virtual_ucc)

            matching_fds = []
            for fd in fds:
                if fd.lhs == frozenset():
                    fd_table = get_table_from_comb(fd.rhs)
                    if fd_table == virtual_ucc_table:
                        matching_fds.append(fd)
                else:
                    if fd.lhs <= virtual_ucc:
                        matching_fds.append(fd)

            if not matching_fds:
                continue

            # Deduplicate by rhs
            seen_rhs = set()
            for fd in matching_fds:
                if fd.rhs not in seen_rhs:
                    seen_rhs.add(fd.rhs)
                    # fd_lhs, fd_rhs, ind_lhs, ind_rhs, ucc_lhs
                    print(f"{fmt(ind.lhs)}, {fmt(ucc.lhs)}, {fmt(fd.rhs)}")
                    all_matches.append((fd.lhs, fd.rhs, projected_lhs, frozenset(ind_lhs), ucc.lhs))

    return all_matches

def t15():
    inds = load_inds(IND_FILE)
    uccs = load_uccs(UCC_FILE)
    fds  = load_fds(FD_FILE)

    # Build rhs index once
    rhs_index: dict[str, list[ColComb]] = {}
    for fd in fds:
        if len(fd.rhs) == 1:
            attr = next(iter(fd.rhs))
            rhs_index.setdefault(attr, []).append(fd.lhs)

    seen_ind = set()
    all_matches = []

    for ucc in uccs:
        ucc_attrs = list(ucc.lhs)

        for ind in inds:
            ind_rhs = list(ind.rhs)  # ordered
            ind_lhs = list(ind.lhs)  # ordered

            # IND rhs must contain ALL ucc_attrs
            if not all(attr in ind_rhs for attr in ucc_attrs):
                continue

            # Project both sides at positions where ucc attrs appear in ind_rhs
            positions = [ind_rhs.index(attr) for attr in ucc_attrs]
            projected_ind_lhs = frozenset(ind_lhs[p] for p in positions)
            projected_ind_rhs = frozenset(ind_rhs[p] for p in positions)  # == ucc.lhs

            key = (ucc.lhs, projected_ind_lhs)
            if key in seen_ind:
                continue
            seen_ind.add(key)

            # projected_ind_lhs acts as virtual UCC for fd_rhs match
            minimal_fd_lhs_list = find_minimal_lhs_for_rhs_union(fds, projected_ind_lhs)
            if not minimal_fd_lhs_list:
                continue

            for fd_lhs in minimal_fd_lhs_list:
                print(f"{fmt(projected_ind_lhs)}, {fmt(projected_ind_rhs)}, {fmt(fd_lhs)}")
                all_matches.append((fd_lhs, projected_ind_rhs, projected_ind_lhs))

    return all_matches

def t16():
    fds  = load_fds(FD_FILE)
    inds = load_inds(IND_FILE)
    uccs = load_uccs(UCC_FILE)

    seen = set()
    all_matches = []

    for ucc in uccs:
        ucc_attrs = list(ucc.lhs)

        for ind in inds:
            ind_lhs = list(ind.lhs)
            ind_rhs = list(ind.rhs)

            # IND rhs must contain ALL ucc attrs
            if not all(attr in ind_rhs for attr in ucc_attrs):
                continue

            # Project IND lhs at positions where ucc attrs appear in IND rhs → X
            positions = [ind_rhs.index(attr) for attr in ucc_attrs]
            projected_x = frozenset(ind_lhs[p] for p in positions)

            # Find FDs whose lhs is subset of or equal to ucc.lhs (Y)
            # and rhs is disjoint from ucc.lhs
            for fd in fds:
                if not fd.lhs <= ucc.lhs:
                    continue
                if not fd.rhs.isdisjoint(ucc.lhs):
                    continue

                key = (ucc.lhs, projected_x, fd.lhs, fd.rhs)
                if key in seen:
                    continue
                seen.add(key)

                print(f"{fmt(projected_x)}, {fmt(ucc.lhs)}, {fmt(fd.lhs)}, {fmt(fd.rhs)}")
                all_matches.append((projected_x, ucc.lhs, fd.lhs, fd.rhs))

    return all_matches

def t17():
    fds  = load_fds(FD_FILE)
    inds = load_inds(IND_FILE)
    uccs = load_uccs(UCC_FILE)

    seen = set()
    all_matches = []

    for ucc in uccs:
        ucc_attrs = list(ucc.lhs)

        # Step 1 — find INDs whose rhs contains all ucc attrs
        matching_inds = []
        for ind in inds:
            ind_rhs = list(ind.rhs)
            ind_lhs = list(ind.lhs)

            if not all(attr in ind_rhs for attr in ucc_attrs):
                continue

            # Project IND lhs at positions where ucc attrs appear in IND rhs → X
            positions = [ind_rhs.index(attr) for attr in ucc_attrs]
            projected_x = frozenset(ind_lhs[p] for p in positions)
            matching_inds.append((projected_x, ind))

        if not matching_inds:
            continue

        # Step 2 — find minimal FD lhs that determines entire UCC via FD rhs
        minimal_lhs_list = find_minimal_lhs_for_rhs_union(fds, ucc.lhs)
        if not minimal_lhs_list:
            continue

        # Step 3 — cross each fd_lhs with each matching IND
        for fd_lhs in minimal_lhs_list:
            for projected_x, ind in matching_inds:

                key = (ucc.lhs, fd_lhs, projected_x)
                if key in seen:
                    continue
                seen.add(key)

                print(f"{fmt(fd_lhs)}, {fmt(ucc.lhs)}, {fmt(projected_x)}")
                all_matches.append((fd_lhs, ucc.lhs, projected_x))

    return all_matches

def t18():
    fds  = load_fds(FD_FILE)
    inds = load_inds(IND_FILE)
    uccs = load_uccs(UCC_FILE)

    seen = set()
    all_matches = []

    for ucc in uccs:
        ucc_attrs = list(ucc.lhs)

        for ind in inds:
            ind_lhs = list(ind.lhs)
            ind_rhs = list(ind.rhs)

            # IND lhs must contain ALL ucc attrs (X)
            if not all(attr in ind_lhs for attr in ucc_attrs):
                continue

            # Project IND rhs at positions where ucc attrs appear in IND lhs → Y
            positions = [ind_lhs.index(attr) for attr in ucc_attrs]
            projected_y = frozenset(ind_rhs[p] for p in positions)

            # FD lhs subset of UCC (X), rhs disjoint from UCC
            for fd in fds:
                if not fd.lhs <= ucc.lhs:
                    continue
                if not fd.rhs.isdisjoint(ucc.lhs):
                    continue

                key = (ucc.lhs, projected_y, fd.lhs, fd.rhs)
                if key in seen:
                    continue
                seen.add(key)

                print(f"{fmt(fd.lhs)}, {fmt(fd.rhs)}, {fmt(projected_y)}, {fmt(ucc.lhs)}")
                all_matches.append((fd.lhs, fd.rhs, projected_y, ucc.lhs))

    return all_matches

def t19():
    fds  = load_fds(FD_FILE)
    inds = load_inds(IND_FILE)
    uccs = load_uccs(UCC_FILE)

    seen = set()
    all_matches = []

    for ucc in uccs:
        ucc_attrs = list(ucc.lhs)

        # Step 1 — find INDs whose lhs contains all ucc attrs
        matching_inds = []
        for ind in inds:
            ind_lhs = list(ind.lhs)
            ind_rhs = list(ind.rhs)

            if not all(attr in ind_lhs for attr in ucc_attrs):
                continue

            positions = [ind_lhs.index(attr) for attr in ucc_attrs]
            projected_y = frozenset(ind_rhs[p] for p in positions)
            matching_inds.append(projected_y)

        if not matching_inds:
            continue

        # Step 2 — find minimal FD lhs that determines entire UCC via FD rhs (Z)
        minimal_lhs_list = find_minimal_lhs_for_rhs_union(fds, ucc.lhs)
        if not minimal_lhs_list:
            continue

        # Step 3 — cross each fd_lhs with each matching IND
        for fd_lhs in minimal_lhs_list:
            for projected_y in matching_inds:

                key = (ucc.lhs, fd_lhs, projected_y)
                if key in seen:
                    continue
                seen.add(key)

                print(f"{fmt(fd_lhs)}, {fmt(ucc.lhs)}, {fmt(projected_y)}")
                all_matches.append((fd_lhs, ucc.lhs, projected_y))

    return all_matches

def t20():
    fds  = load_fds(FD_FILE)
    inds = load_inds(IND_FILE)
    uccs = load_uccs(UCC_FILE)

    seen = set()
    all_matches = []

    for ucc in uccs:
        ucc_attrs = list(ucc.lhs)

        for ind in inds:
            ind_lhs = list(ind.lhs)
            ind_rhs = list(ind.rhs)

            # IND lhs must contain ALL ucc attrs (X)
            if not all(attr in ind_lhs for attr in ucc_attrs):
                continue

            # Project IND rhs → Y
            positions = [ind_lhs.index(attr) for attr in ucc_attrs]
            projected_y = frozenset(ind_rhs[p] for p in positions)

            # FD lhs subset of projected_y (Y), rhs disjoint from projected_y
            for fd in fds:
                if not fd.lhs <= projected_y:
                    continue
                if not fd.rhs.isdisjoint(projected_y):
                    continue

                key = (ucc.lhs, projected_y, fd.lhs, fd.rhs)
                if key in seen:
                    continue
                seen.add(key)

                print(f"{fmt(ucc.lhs)}, {fmt(projected_y)}, {fmt(fd.lhs)}, {fmt(fd.rhs)}")
                all_matches.append((ucc.lhs, projected_y, fd.lhs, fd.rhs))

    return all_matches

def t21():
    fds  = load_fds(FD_FILE)
    inds = load_inds(IND_FILE)
    uccs = load_uccs(UCC_FILE)

    seen = set()
    all_matches = []

    for ucc in uccs:
        ucc_attrs = list(ucc.lhs)

        for ind in inds:
            ind_lhs = list(ind.lhs)
            ind_rhs = list(ind.rhs)

            # IND lhs must contain ALL ucc attrs (X)
            if not all(attr in ind_lhs for attr in ucc_attrs):
                continue

            # Project IND rhs → Y
            positions = [ind_lhs.index(attr) for attr in ucc_attrs]
            projected_y = frozenset(ind_rhs[p] for p in positions)

            # FD rhs matches projected_y (Y) → find minimal lhs (Z)
            minimal_lhs_list = find_minimal_lhs_for_rhs_union(fds, projected_y)
            if not minimal_lhs_list:
                continue

            for fd_lhs in minimal_lhs_list:
                key = (ucc.lhs, projected_y, fd_lhs)
                if key in seen:
                    continue
                seen.add(key)

                print(f"{fmt(ucc.lhs)}, {fmt(projected_y)}, {fmt(fd_lhs)}")
                all_matches.append((ucc.lhs, projected_y, fd_lhs))

    return all_matches

def t22():
    fds  = load_fds(FD_FILE)
    inds = load_inds(IND_FILE)
    uccs = load_uccs(UCC_FILE)

    seen = set()
    all_matches = []

    for ucc in uccs:
        # Step 1 — find minimal FD lhs that determines entire UCC via FD rhs
        minimal_lhs_list = find_minimal_lhs_for_rhs_union(fds, ucc.lhs)
        if not minimal_lhs_list:
            continue

        for fd_lhs in minimal_lhs_list:
            fd_lhs_attrs = list(fd_lhs)

            # Step 2 — find INDs whose lhs is superset or equal of fd_lhs
            for ind in inds:
                ind_lhs = list(ind.lhs)
                ind_rhs = list(ind.rhs)

                # IND lhs must contain ALL fd_lhs attrs
                if not all(attr in ind_lhs for attr in fd_lhs_attrs):
                    continue

                # Project IND rhs at positions where fd_lhs attrs appear in IND lhs
                projected_rhs = frozenset(ind_rhs[ind_lhs.index(attr)] for attr in fd_lhs_attrs)

                key = (ucc.lhs, fd_lhs, projected_rhs)
                if key in seen:
                    continue
                seen.add(key)

                print(f"{fmt(fd_lhs)}, {fmt(ucc.lhs)}, {fmt(projected_rhs)}")
                all_matches.append((fd_lhs, ucc.lhs, projected_rhs))

    return all_matches

def t23():
    fds  = load_fds(FD_FILE)
    inds = load_inds(IND_FILE)
    uccs = load_uccs(UCC_FILE)

    # Build IND lhs index — single attr → list of INDs
    ind_lhs_index = {}
    for ind in inds:
        for attr in ind.lhs:
            ind_lhs_index.setdefault(attr, []).append(ind)

    seen = set()
    all_matches = []

    for ucc in uccs:
        # Find FDs whose lhs is subset of UCC and rhs is disjoint from UCC
        for fd in fds:
            if not fd.lhs <= ucc.lhs:
                continue
            if not fd.rhs.isdisjoint(ucc.lhs):
                continue

            # FD rhs is single attribute — get it
            if len(fd.rhs) != 1:
                continue
            fd_rhs_attr = next(iter(fd.rhs))

            # Match FD rhs with IND lhs
            for ind in ind_lhs_index.get(fd_rhs_attr, []):
                ind_lhs = list(ind.lhs)
                ind_rhs = list(ind.rhs)

                # Project IND rhs at position where fd_rhs_attr appears in IND lhs
                pos = ind_lhs.index(fd_rhs_attr)
                projected_rhs = ind_rhs[pos]

                key = (ucc.lhs, fd.rhs, frozenset({projected_rhs}))
                if key in seen:
                    continue
                seen.add(key)

                print(f"{fmt(fd.rhs)}, [{projected_rhs}], {fmt(ucc.lhs)}")
                all_matches.append((ucc.lhs, fd.rhs, frozenset({projected_rhs})))

    return all_matches

def t24():
    fds  = load_fds(FD_FILE)
    inds = load_inds(IND_FILE)
    uccs = load_uccs(UCC_FILE)

    seen = set()
    all_matches = []

    for ucc in uccs:
        # Step 1 — find minimal FD lhs that determines entire UCC via FD rhs
        minimal_lhs_list = find_minimal_lhs_for_rhs_union(fds, ucc.lhs)
        if not minimal_lhs_list:
            continue

        for fd_lhs in minimal_lhs_list:
            fd_lhs_attrs = list(fd_lhs)

            # Step 2 — find INDs whose rhs is superset or equal of fd_lhs
            for ind in inds:
                ind_lhs = list(ind.lhs)
                ind_rhs = list(ind.rhs)

                # IND rhs must contain ALL fd_lhs attrs
                if not all(attr in ind_rhs for attr in fd_lhs_attrs):
                    continue

                # Project IND lhs at positions where fd_lhs attrs appear in IND rhs
                projected_lhs = frozenset(ind_lhs[ind_rhs.index(attr)] for attr in fd_lhs_attrs)

                key = (ucc.lhs, fd_lhs, projected_lhs)
                if key in seen:
                    continue
                seen.add(key)

                print(f"{fmt(fd_lhs)}, {fmt(ucc.lhs)}, {fmt(projected_lhs)}")
                all_matches.append((fd_lhs, ucc.lhs, projected_lhs))

    return all_matches

def t25():
    fds  = load_fds(FD_FILE)
    inds = load_inds(IND_FILE)
    uccs = load_uccs(UCC_FILE)

    # Build IND rhs index — single attr → list of INDs
    ind_rhs_index = {}
    for ind in inds:
        for attr in ind.rhs:
            ind_rhs_index.setdefault(attr, []).append(ind)

    seen = set()
    all_matches = []

    for ucc in uccs:
        for fd in fds:
            if not fd.lhs <= ucc.lhs:
                continue
            if not fd.rhs.isdisjoint(ucc.lhs):
                continue

            if len(fd.rhs) != 1:
                continue
            fd_rhs_attr = next(iter(fd.rhs))

            # Match FD rhs with IND rhs
            for ind in ind_rhs_index.get(fd_rhs_attr, []):
                ind_lhs = list(ind.lhs)
                ind_rhs = list(ind.rhs)

                # Project IND lhs at position where fd_rhs_attr appears in IND rhs
                pos = ind_rhs.index(fd_rhs_attr)
                projected_lhs = ind_lhs[pos]

                key = (ucc.lhs, fd.rhs, frozenset({projected_lhs}))
                if key in seen:
                    continue
                seen.add(key)

                print(f"{fmt(fd.rhs)}, [{projected_lhs}], {fmt(ucc.lhs)}")
                all_matches.append((ucc.lhs, fd.rhs, frozenset({projected_lhs})))

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

# ── DISPATCH ──────────────────────────────────────────────────────────────────
# Only set the files your query actually needs (others can be left as None)
DATASET = "dataset_results/TPCH_12"
FD_FILE  = f"{DATASET}/fd.txt"
UCC_FILE = f"{DATASET}/ucc.txt"
IND_FILE = f"{DATASET}/ind.txt"

QUERY = "T1"

QUERIES = {
    "T1": t1,
    "T3": t3,
    "T4": t4,
    "T5": t5,
    "T14": t14,
    "T15": t15,
    "T16": t16,
    "T17": t17,
    "T18": t18,
    "T19": t19,
    "T20": t20,
    "T21": t21,
    "T22": t22,
    "T23": t23,
    "T24": t24,
    "T25": t25
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

if __name__ == "__main__":
    run_query(QUERY)