from dataclasses import dataclass

ColComb = frozenset  # frozenset[str]

@dataclass(frozen=True)
class UCC:
    lhs: ColComb

@dataclass(frozen=True)
class FD:
    lhs: ColComb
    rhs: ColComb

@dataclass(frozen=True)
class IND:
    lhs: tuple  # ordered, positional
    rhs: tuple  # ordered, positional