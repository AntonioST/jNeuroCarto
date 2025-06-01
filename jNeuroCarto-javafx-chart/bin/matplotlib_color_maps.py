from matplotlib import colormaps

n = 25

names = [
    'Accent', 'afmhot', 'autumn',
    'binary', 'Blues', 'bone', 'BrBG', 'brg', 'BuGn', 'BuPu', 'bwr',
    'CMRmap', 'cividis', 'cool', 'coolwarm', 'copper', 'cubehelix',
    'Dark2',
    'flag',
    'gist_earth', 'gist_gray', 'gist_heat', 'gist_ncar', 'gist_rainbow', 'gist_stern', 'gist_yarg',
    'GnBu', 'gnuplot', 'gnuplot2', 'gray', 'Greens', 'Greys',
    'hot', 'hsv',
    'inferno',
    'jet',
    'magma',
    'nipy_spectral',
    'ocean', 'OrRd', 'Oranges',
    'PRGn', 'Paired', 'Pastel1', 'Pastel2',
    'PiYG', 'pink', 'plasma', 'prism', 'PuBu', 'PuBuGn', 'PuOr', 'PuRd', 'Purples',
    'rainbow', 'RdBu', 'RdGy', 'RdPu', 'RdYlBu', 'RdYlGn', 'Reds',
    'seismic', 'Set1', 'Set2', 'Set3', 'Spectral', 'spring', 'summer',
    'tab10', 'tab20', 'tab20b', 'tab20c', 'terrain', 'turbo', 'twilight', 'twilight_shifted',
    'viridis',
    'winter', 'Wistia',
    'YlGn', 'YlGnBu', 'YlOrBr', 'YlOrRd',
]

print(f"""\
\"""
Generate by matplotlib_color_maps.py
\"""
""")
for name in names:
    if isinstance(name, str):
        cm = colormaps[name]
        print(name, '= [')
    else:
        for n in name:
            cm = colormaps[n]
        print(' = '.join(name), '= [')

    for i in range(n + 1):
        c = cm(i / n)
        print('  ', ', '.join(map('%.4f'.__mod__, [i / n, c[0], c[1], c[2]])), end=',\n')

    print(']')
    print()
