import re

def read(path: str) -> list[str]:
    with open(path, encoding='utf-8-sig') as f:
        return [line.strip() for line in f.read().splitlines() if line.strip()]

def parse_line(line: str) -> tuple:
    groups = re.findall(r'\[([^\]]*)\]', line)
    return tuple(
        frozenset(item.strip() for item in group.split(',') if item.strip())
        for group in groups
    )

def filter_csv(filepath, column, target_values):
    target = frozenset(target_values)

    lines = read(filepath)
    for line in lines:
        cells = parse_line(line)
        if column >= len(cells):
            continue
        if target.issubset(cells[column]):
            print(line)


if __name__ == '__main__':
    filepath = "dataset_results/TPCH_12/b1_holl_0_3.txt"
    column   = 0
    values   = ["lineitem.L_PARTKEY", "lineitem.L_SUPPKEY"]  # list — not a string

    filter_csv(filepath, column, values)