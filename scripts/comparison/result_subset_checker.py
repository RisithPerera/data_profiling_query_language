import re

FILE = "../backup/fd_results/TPCH_HOLL_Only.txt"
FILTER = "[L_TAX, L_COMMENT, L_RETURNFLAG, L_SUPPKEY, L_LINENUMBER]"


def parse_first_set(line):
    match = re.search(r'\[([^\]]+)\]', line)
    if not match:
        return None
    return frozenset(item.strip() for item in match.group(1).split(','))


def parse_filter(filter_str):
    match = re.search(r'\[([^\]]+)\]', filter_str)
    if not match:
        raise ValueError("Invalid filter format")
    return frozenset(item.strip() for item in match.group(1).split(','))


def read(path):
    return [line for line in open(path).read().splitlines() if line.strip()]


filter_set = parse_filter(FILTER)
lines = read(FILE)

matched = []
not_matched = []

for line in lines:
    first_set = parse_first_set(line)
    if first_set is None:
        continue
    if first_set.issubset(filter_set):
        matched.append(line)
    else:
        not_matched.append(line)

print(f"=== Matched (first set is subset of filter) === Size: {len(matched)}")
for line in matched:
    print(line)

print(f"\n=== Not Matched === Size: {len(not_matched)}")
for line in not_matched:
    print(line)