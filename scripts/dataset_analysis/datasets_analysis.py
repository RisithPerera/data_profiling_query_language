import os
import csv


def collect_csv_info(root_folder):
    result = []

    for dirpath, dirnames, filenames in os.walk(root_folder):
        for filename in filenames:
            if filename.lower().endswith('.csv'):
                file_path = os.path.join(dirpath, filename)

                # Determine first-level folder
                relative_path = os.path.relpath(file_path, root_folder)
                parts = relative_path.split(os.sep)
                first_folder = parts[0] if len(parts) > 1 else ''

                # Get file size
                file_size = os.path.getsize(file_path)

                # Count rows and columns
                encodings = ['utf-8', 'latin1', 'cp1252']  # common alternatives
                row_count = 0
                col_count = 0

                for enc in encodings:
                    try:
                        with open(file_path, 'r', newline='', encoding=enc) as f:
                            sample = f.read(1024)
                            f.seek(0)
                            try:
                                dialect = csv.Sniffer().sniff(sample, delimiters=[',', ';', '\t', '|', ' '])
                            except csv.Error:
                                dialect = csv.excel
                            reader = csv.reader(f, dialect)
                            for row in reader:
                                row_count += 1
                                if row_count == 1:
                                    col_count = len(row)
                        break  # success, stop trying encodings
                    except Exception:
                        continue  # try next encoding
                else:
                    print(f"Failed to read {file_path} with available encodings")

                size_mb = round(file_size / (1024 * 1024), 2)
                print(f"{first_folder}, {filename}, {row_count}, {col_count}, {size_mb}")

                result.append({
                    'dataset': first_folder,
                    'file': filename,
                    'rows': row_count,
                    'columns': col_count,
                    'size': size_mb,
                    'delimiter': dialect.delimiter
                })

    return result


def save_summary_to_csv(summary, output_file):
    import pandas as pd
    df = pd.DataFrame(summary)
    df.to_csv(output_file, index=False)


if __name__ == "__main__":
    folder_to_scan = "../datasets"
    summary = collect_csv_info(folder_to_scan)
    output_csv = "csv_summary_v2.csv"
    save_summary_to_csv(summary, output_csv)
    print(f"Summary saved to {output_csv}")
