import re

DATASET = "dataset_results/Sakila"
FILE = f"{DATASET}/ind_holl.txt"

def read_lines(path: str) -> list[str]:
    with open(path, encoding='utf-8-sig') as f:
        return [line.strip() for line in f.read().splitlines() if line.strip()]

def parse_line(line: str) -> tuple[frozenset, frozenset]:
    groups = [frozenset(item.strip() for item in m.split(',') if item.strip())
              for m in re.findall(r'\[([^\]]*)\]', line)]
    return (groups[0], groups[1])

def is_maximal(lhs: frozenset, rhs: frozenset, all_pairs: list[tuple]) -> bool:
    for other_lhs, other_rhs in all_pairs:
        if (other_lhs, other_rhs) == (lhs, rhs):
            continue
        if lhs <= other_lhs and rhs <= other_rhs:
            return False
    return True

lines  = read_lines(FILE)
pairs  = [parse_line(l) for l in lines]

maximal = [(l, lhs, rhs) for l, (lhs, rhs) in zip(lines, pairs)
           if is_maximal(lhs, rhs, pairs)]

for line, _, _ in maximal:
    print(line)