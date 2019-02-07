import React, { Component } from 'react';
import PropTypes from 'prop-types';
import { Button, GET_ONE, UPDATE } from 'react-admin';
import { dataProvider } from '../App';
import Ring from "ringjs";
import { TimeSeries, TimeRange, TimeEvent } from "pondjs";

import {
  ChartContainer,
  ChartRow,
  Charts,
  YAxis,
  LineChart,
  Baseline,
  Resizable
} from "react-timeseries-charts";

import { red100, red500, green100, green500, blueA700, greenA700, amberA700, redA700 } from '@material-ui/core/colors';

import Divider from '@material-ui/core/Divider';
import Switch from '@material-ui/core/Switch';

import CellIcon from '@material-ui/icons/NetworkCell';
import CellSignal0Icon from '@material-ui/icons/SignalCellular0Bar';
import CellSignal1Icon from '@material-ui/icons/SignalCellular1Bar';
import CellSignal2Icon from '@material-ui/icons/SignalCellular2Bar';
import CellSignal3Icon from '@material-ui/icons/SignalCellular3Bar';
import CellSignal4Icon from '@material-ui/icons/SignalCellular4Bar';
import { green600, blue600, orange600, red600, grey600 } from '@material-ui/core/colors';

const second = 1000;
const minute = 60 * second;

const relaystyle = {
    value: { normal: {stroke: blueA700, fill: "none", strokeWidth: 2} },
    labels: { labelColor: blueA700 },
    axis: { axisColor: blueA700 }
};
const inputstyle = {
    value: { normal: {stroke: amberA700, fill: "none", strokeWidth: 2} },
    labels: { labelColor: amberA700 },
    axis: { axisColor: amberA700 }
};
const baselineOnStyle = {
    line: {
        stroke: green500,
        strokeWidth: 0.5
    }
};
const baselineOffStyle = {
    line: {
        stroke: red500,
        strokeWidth: 0.5
    }
};
const dateStyle = {
    fontSize: 16,
    color: "#AAA",
    borderWidth: 1,
    borderColor: "#F4F4F4"
};
const togglestyles = {
    block: {
        maxWidth: '8em',
        display: 'inline-block',
        marginRight: '1em'
    },
    toggle: {
        maxWidth: '8em',
        display: 'inline-block',
        marginRight: '1em',
        fontSize: '16px',
        marginBottom: '1em',
    },
    thumbOff: {
        backgroundColor: red500,
    },
    trackOff: {
        backgroundColor: red100,
    },
    thumbSwitched: {
        backgroundColor: green500,
    },
    trackSwitched: {
        backgroundColor: green100,
    }
}
const styles = {
    shim_1em: { display: 'inline-block', width: '1em' },
    inline: { display: 'inline-block', fontSize: '16px' }
};

const RelayLabel = ({record}) => (
    record
    ?
    <span style={styles.inline}>
        {record.connected
        ? (
        `${record.label} - connected to ${record.contacts === 'NO' ? `Normal Open` : `Normal Close`} contacts.`
        )
        : `Not Connected` }
        </span>
    :
    <span/>
);

const getCellIcon = (ss) => {
    switch (ss) {
        case 1:
            return <CellSignal0Icon color={grey600} />;
        case 2:
            return <CellSignal1Icon color={red600} />;
        case 3:
            return <CellSignal2Icon color={orange600} />;
        case 4:
            return <CellSignal3Icon color={blue600} />;
        case 5:
            return <CellSignal4Icon color={green600} />;
        default:
            return <CellIcon />
    }
};

class TestCube extends Component {
    constructor(props) {
        super(props);
        this.state = {
            time: props.startTime,
            r: new Ring(120),
            i: new Ring(120),
            ss: 0,
            relay_switch: false,
            data: {}
        };

        this.onRelayToggle = this.onRelayToggle.bind(this);
    }

    onRelayToggle = event => {
        dataProvider(UPDATE, 'test', {
            id: this.props.cubeID,
            data: { relay: event.target.checked }
        });
    };

    componentWillMount() {
        dataProvider(GET_ONE, 'setup', {
            id: this.props.cubeID
        })
        .then(response => response.data)
        .then(data => {
            if (data) {
                this.setState({
                    data
                });
            }
        });

        this.interval = setInterval(() => {
            const t = new Date(this.state.time.getTime() + second);

            dataProvider(GET_ONE, 'test', {
                id: this.props.cubeID
            })
            .then(response => response.data)
            .then(data => {
                if (data) {
                    // Relay events
                    const relayEvents = this.state.r;
                    relayEvents.push(new TimeEvent(t, data.r));
                    // Input events
                    const inputEvents = this.state.i;
                    inputEvents.push(new TimeEvent(t, data.i));
                    // Signal Streght
                    const ss = data.ss;

                    this.setState({
                        time: t,
                        r: relayEvents,
                        i: inputEvents,
                        ss: ss,

                        relay_switch: (data.r === 1 ? true : false),
                    });
                }
            });
        }, second );
    };

    componentWillUnmount() {
        clearInterval(this.interval);
    }

    componentDidMount() {
        setTimeout(() => {
            window.dispatchEvent(new Event('resize'));
        }, 0);
    }

    render() {
        const { startTime } = this.props;
        const latestTime = `${this.state.time}`;
        const { relay_switch, data, ss } = this.state;

        // Series data for each chart
        const relaySeries = new TimeSeries({
            name: "relay",
            events: this.state.r.toArray()
        });
        const inputSeries = new TimeSeries({
            name: "input",
            events: this.state.i.toArray()
        });

        // Timerange for the chart axis
        const initialBeginTime = startTime;
        const timeWindow = 2 * minute;

        let beginTime;
        const endTime = new Date(this.state.time.getTime() + second);
        if (endTime.getTime() - timeWindow < initialBeginTime.getTime()) {
            beginTime = initialBeginTime;
        } else {
            beginTime = new Date(endTime.getTime() - timeWindow);
        }
        const timeRange = new TimeRange(beginTime, endTime);

        return (
            <div>
                <div>
                    <span>
                        {getCellIcon(ss)}
                        <span style={ styles.shim_1em }>&nbsp;</span>
                            Signal Strength - {ss} of 5
                    </span>
                </div>

                <Divider style={{ marginTop: '1em' }} />

                <div>
                    <Switch
                        checked={relay_switch}
                        onChange={this.onRelayToggle}
                        value={relay_switch}
                        color="primary"
                    />
                    <RelayLabel record={data.relay} />
                </div>

                <Divider style={{ marginBottom: '1em' }} />

                <div style={{ marginBottom: '1em' }}>
                    <span style={dateStyle}>{latestTime}</span>
                </div>

                <Resizable>
                    <ChartContainer timeRange={timeRange} showGrid showGridPosition="under">
                        <ChartRow height="100">
                            <YAxis
                                id="relay"
                                label="RELAY"
                                min={0} max={1}
                                tickCount={1}
                                format=",.0f"
                                type="linear"
                                style={relaystyle}
                            />
                            <Charts>
                                <LineChart
                                    axis="relay"
                                    series={relaySeries}
                                    interpolation="curveStep"
                                    style={relaystyle}
                                />
                                <Baseline
                                    axis="relay"
                                    value={0.07}
                                    label="OFF"
                                    style={baselineOffStyle}
                                />
                                <Baseline
                                    axis="relay"
                                    value={0.93}
                                    label="ON"
                                    style={baselineOnStyle}
                                />
                            </Charts>
                        </ChartRow>
                        <ChartRow height="100">
                            <YAxis
                                id="input"
                                label="INPUT"
                                min={0} max={1}
                                tickCount={1}
                                format=",.0f"
                                type="linear"
                                style={inputstyle}
                            />
                            <Charts>
                                <LineChart
                                    axis="input"
                                    series={inputSeries}
                                    interpolation="curveStep"
                                    style={inputstyle}
                                />
                                <Baseline
                                    axis="input"
                                    value={0.07}
                                    label="LOW"
                                    style={baselineOffStyle}
                                />
                                <Baseline
                                    axis="input"
                                    value={0.93}
                                    label="HIGH"
                                    style={baselineOnStyle}
                                />
                            </Charts>
                        </ChartRow>
                    </ChartContainer>
                </Resizable>
            </div>
        );
    }
}

TestCube.propTypes = {
  cubeID: PropTypes.string,
  startTime: PropTypes.object
};

export default TestCube;
