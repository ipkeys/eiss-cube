import React, { Component, Fragment } from 'react';
import PropTypes from 'prop-types';
import { withStyles } from '@material-ui/core/styles';
import moment from 'moment';
import { Button, CREATE, GET_ONE, UPDATE } from 'react-admin';
import { dataProvider } from '../App';
import Ring from "ringjs";
import { 
    TimeSeries, 
    TimeRange, 
    TimeEvent
} from "pondjs";

import {
  ChartContainer,
  ChartRow,
  Charts,
  YAxis,
  LineChart,
  Baseline,
  Resizable
} from "react-timeseries-charts";

import { red100, red500, green100, green500, blueA700, amberA700 } from '@material-ui/core/colors';

import LinearProgress from '@material-ui/core/LinearProgress';
import Divider from '@material-ui/core/Divider';

import StartTestIcon from '@material-ui/icons/PlayCircleOutline';
import CellIcon from '@material-ui/icons/NetworkCell';
import CellSignal0Icon from '@material-ui/icons/SignalCellular0Bar';
import CellSignal1Icon from '@material-ui/icons/SignalCellular1Bar';
import CellSignal2Icon from '@material-ui/icons/SignalCellular2Bar';
import CellSignal3Icon from '@material-ui/icons/SignalCellular3Bar';
import CellSignal4Icon from '@material-ui/icons/SignalCellular4Bar';
import { green600, blue600, orange600, red600, grey600 } from '@material-ui/core/colors';

const second = 1000;
const minute = 60000;

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

const styles = theme => ({
	btnPadding: {
        paddingRight: theme.spacing.unit
    },
    title: {
        display: 'inline-flex',
        alignItems: 'center',
    },
    content: {
        paddingLeft: theme.spacing.unit * 3,
        paddingRight: theme.spacing.unit * 3,
        paddingBottom: 0
	}
});


const getCellIcon = (classes, ss) => {
    switch (ss) {
        case 1:
            return <CellSignal0Icon className={classes.btnPadding} color={grey600} />;
        case 2:
            return <CellSignal1Icon className={classes.btnPadding} color={red600} />;
        case 3:
            return <CellSignal2Icon className={classes.btnPadding} color={orange600} />;
        case 4:
            return <CellSignal3Icon className={classes.btnPadding} color={blue600} />;
        case 5:
            return <CellSignal4Icon className={classes.btnPadding} color={green600} />;
        default:
            return <CellIcon className={classes.btnPadding} />
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
            completed: 0
        };
    }

    startTest = () => {
        dataProvider(CREATE, 'test', {
            data: {cubeID: this.props.cubeID}
        });

        this.timer = setInterval(this.progress, 2000);
    };

    progress = () => {
        const { completed } = this.state;
        if (completed === 100) {
            clearInterval(this.timer);
            // TODO: 


        } else {
            const diff = Math.random() * 10;
            this.setState({ completed: Math.min(completed + diff, 100) });
        }
    };

    componentWillMount() {
        this.interval = setInterval(() => {
            let t = moment();

            dataProvider(GET_ONE, 'test', {
                id: this.props.cubeID
            })
            .then(response => response.data)
            .then(data => {
                if (data) {
                    const relayEvents = this.state.r;
                    relayEvents.push(new TimeEvent(t, data.r));
                    
                    const inputEvents = this.state.i;
                    inputEvents.push(new TimeEvent(t, data.i));

                    this.setState({
                        time: t,
                        r: relayEvents,
                        i: inputEvents,
                        ss: data.ss // Signal Streght
                    });
                }
            });
        }, 5 * second);
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
        const { classes, startTime } = this.props;
        const { time, r, i, ss } = this.state;

        // Series data for each chart
        const relaySeries = new TimeSeries({
            name: "relay",
            events: r.toArray()
        });
        const inputSeries = new TimeSeries({
            name: "input",
            events: i.toArray()
        });

        // Timerange for the chart axis
        const initialBeginTime = startTime;
        const timeWindow = 2 * minute;

        const endTime = moment().add(5, 's'); // new Date(time.getTime() + 5 * second);
        let beginTime;
        if (endTime.seconds() - timeWindow < initialBeginTime.seconds()) {
            beginTime = initialBeginTime;
        } else {
            beginTime = moment().subtract(timeWindow, 'm');// new Date(endTime.getTime() - timeWindow);
        }
        const timeRange = new TimeRange(beginTime, endTime);

        return (
            <div>
                <Fragment>
                    <span className={classes.title}>
                        {getCellIcon(classes, ss)}
                        Signal Strength - {ss} of 5
                    </span>
                </Fragment>

                <Divider style={{ marginTop: '1em' }} />

                <Fragment>
                    <Button label='START TEST' color="primary" onClick={this.startTest} >
                        <StartTestIcon />
                    </Button>
                </Fragment>

                <Divider style={{ marginBottom: '1em' }} />

                <Fragment>
                    <LinearProgress variant="determinate" value={this.state.completed} />
                </Fragment>

                <Divider style={{ marginBottom: '1em' }} />

                <Resizable>
                    <ChartContainer timeRange={timeRange} showGrid showGridPosition="under">
                        <ChartRow height="100">
                            <YAxis
                                id="relay"
                                label="RELAY"
                                min={0} max={1}
                                width="50"
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
{/* 
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
 */}
                            </Charts>
                        </ChartRow>
                        <ChartRow height="100">
                            <YAxis
                                id="input"
                                label="INPUT"
                                min={0} max={1}
                                width="50"
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
{/* 
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
*/}
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

export default withStyles(styles)(TestCube);
