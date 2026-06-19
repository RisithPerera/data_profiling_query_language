import pandas as pd

# Load the per-file CSV summary
df = pd.read_csv("backup/dataset_analysis/csv_summary_v2.csv")

# Dataset-level aggregation
dataset_summary = (
    df.groupby("dataset")
      .agg(
          num_relations=("file", "count"),
          total_size_mb=("size", "sum"),
          total_rows=("rows", "sum"),
          total_columns=("columns", "sum"),
          max_rows=("rows", "max"),
          max_columns=("columns", "max"),
          avg_rows=("rows", "mean"),
          avg_columns=("columns", "mean"),
      )
      .reset_index()
)

# Round numeric columns
dataset_summary["avg_rows"]      = dataset_summary["avg_rows"].round(2)
dataset_summary["avg_columns"]   = dataset_summary["avg_columns"].round(2)
dataset_summary["total_size_mb"] = dataset_summary["total_size_mb"].round(2)

# Reorder columns
dataset_summary = dataset_summary[[
    "dataset", "total_size_mb", "num_relations",
    "total_rows", "total_columns",
    "max_rows", "max_columns",
    "avg_rows", "avg_columns"
]]

# Save
dataset_summary.to_csv("dataset_level_summary_v2.csv", index=False)
print(dataset_summary.to_string(index=False))

# File with maximum rows per dataset
df.loc[df.groupby("dataset")["rows"].idxmax(), ["dataset", "file", "rows"]] \
  .to_csv("dataset_max_rows_files.csv", index=False)

# File with maximum columns per dataset
df.loc[df.groupby("dataset")["columns"].idxmax(), ["dataset", "file", "columns"]] \
  .to_csv("dataset_max_columns_files.csv", index=False)