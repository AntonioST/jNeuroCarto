import Plotly from 'plotly.js';
import { ChartEndpoint } from 'Frontend/generated/endpoints';

export default function init() {
    ChartEndpoint.test().then((text: string) => console.log(`ChartEndpoint.test => {text}`));

    var trace1 = {
        x: [1, 2, 3, 4],
        y: [10, 15, 13, 17],
        mode: 'markers',
        type: 'scatter'
    };

    var trace2 = {
        x: [2, 3, 4, 5],
        y: [16, 5, 11, 9],
        mode: 'lines',
        type: 'scatter'
    };

    var trace3 = {
        x: [1, 2, 3, 4],
        y: [12, 9, 15, 12],
        mode: 'lines+markers',
        type: 'scatter'
    };

    var data = [trace1, trace2, trace3];

    Plotly.newPlot('jneurocarto-chart', data);

    return (
        <div id="jneurocarto-chart">
        </div>
    )
}