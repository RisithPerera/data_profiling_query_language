import re

FILE1 = "dataset_results/Sakila/t12_dpal.txt"
FILE2 = "dataset_results/Sakila/t12_holl.txt"


def read(path: str) -> list[str]:
    with open(path, encoding='utf-8-sig') as f:
        return [line.strip() for line in f.read().splitlines() if line.strip()]

def parse_line(line: str) -> tuple:
    groups = re.findall(r'\[([^\]]*)\]', line)
    return tuple(
        frozenset(item.strip() for item in group.split(',') if item.strip())
        for group in groups
    )

lines1 = read(FILE1)
lines2 = read(FILE2)

norm1 = [parse_line(l) for l in lines1]
norm2 = [parse_line(l) for l in lines2]

norm1_set = set(norm1)
norm2_set = set(norm2)

common = [l for l, n in zip(lines1, norm1) if n in norm2_set]
only1  = [l for l, n in zip(lines1, norm1) if n not in norm2_set]
only2  = [l for l, n in zip(lines2, norm2) if n not in norm1_set]

print(f"=== Common === Size: {len(common)}")
for line in common:
    print(line)

print(f"\n=== Only in {FILE1} === Size: {len(only1)}")
for line in only1:
    print(line)

print(f"\n=== Only in {FILE2} === Size: {len(only2)}")
for line in only2:
    print(line)