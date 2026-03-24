import argparse
import matplotlib.pyplot as plt
import numpy as np
import polars as pl
import re
import sys
from pathlib import Path

import io


def read_benchmark_result(path: Path) -> pl.DataFrame:
    content = path.read_text()
    content = content.replace('±', '')
    content = re.sub(r' +', ',', content)
    return pl.read_csv(io.StringIO(content), separator=',', has_header=True, comment_prefix='#')


def processing_benchmark_name(df: pl.DataFrame) -> pl.DataFrame:
    df = df.with_columns(
        pl.col('Benchmark').str.replace(r'.+?\.', '').alias('Benchmark')
    ).with_columns(
        pl.col('Benchmark').str.replace(r'measure_', '').alias('Benchmark')
    )
    return df


def plot_benchmark(df: pl.DataFrame, output: str = None):
    fig, ax = plt.subplots()

    benchmarks = df['Benchmark'].unique(maintain_order=True)
    code = df['(code)'].unique(maintain_order=True)
    n_code = len(code)

    x = np.arange(len(benchmarks))
    w = 0.8 / len(code)

    for i, c in enumerate(code):
        arr = df.filter(pl.col('(code)') == c)['Score'].to_numpy()
        ax.bar(x + i * w, arr, w, label=c)

    info = df.row(0, named=True)

    ax.grid(axis='y')
    ax.set_ylabel('%s %s' % (info['Mode'], info['Units']))
    ax.set_xticks(x, benchmarks, rotation=90)
    ax.legend()

    fig.tight_layout()

    if output is None:
        plt.show()
    else:
        plt.savefig(output, dpi=300)


def main():
    AP = argparse.ArgumentParser()
    AP.add_argument('FILE', help='benchmark result file')
    AP.add_argument('OUTPUT', nargs='?', default=None, help='output figure')
    opt = AP.parse_args()

    output = opt.OUTPUT
    if output is not None:
        if not output.endswith('.png'):
            output = output + '.png'

    df = read_benchmark_result(Path(opt.FILE))
    df = processing_benchmark_name(df)
    plot_benchmark(df, output)


if __name__ == '__main__':
    main()
