import numpy as np
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

data = {}
for name in names:
    cm = colormaps[name]
    a = []
    for i in range(n + 1):
        c = cm(i / n)
        a.append((i / n, *c))
    data[name] = np.array(a)

np.savez('matplotlib_color_maps.npz', **data)
