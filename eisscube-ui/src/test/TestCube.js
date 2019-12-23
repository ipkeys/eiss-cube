import React, { Component } from 'react';
import PropTypes from 'prop-types';
import { withStyles } from '@material-ui/core/styles';
import moment from 'moment';
import { Button, CREATE, GET_ONE } from 'react-admin';
import { dataProvider } from '../globalExports';
import { TimeSeries, TimeRange} from "pondjs";
import {
  ChartContainer,
  ChartRow,
  Charts,
  YAxis,
  LineChart,
  Baseline,
  Resizable
} from "react-timeseries-charts";

import LinearProgress from '@material-ui/core/LinearProgress';
import ExpansionPanel from '@material-ui/core/ExpansionPanel';
import ExpansionPanelDetails from '@material-ui/core/ExpansionPanelDetails';
import ExpansionPanelSummary from '@material-ui/core/ExpansionPanelSummary';
import Typography from '@material-ui/core/Typography';
import ExpandMoreIcon from '@material-ui/icons/ExpandMore';
import Divider from '@material-ui/core/Divider';

import StartTestIcon from '@material-ui/icons/PlayCircleOutline';
import { green, red, blue, amber, yellow, grey } from '@material-ui/core/colors';

import SelectDuration from './SelectDuration';
import SelectCycle from './SelectCycle';

const relayStyle = {
    value: { 
        normal: {
            stroke: blue[800], 
            fill: 'none', 
            strokeWidth: 2
        } 
    },
    label: {
        fill: blue[800]
    }
};

const inputStyle = {
    value: { 
        normal: {
            stroke: amber[800], 
            fill: 'none', 
            strokeWidth: 2
        } 
    },
    label: {
        fill: amber[800]
    }
};

const baselineUpStyle = {
    line: {
        stroke: green[500],
        strokeWidth: 1,
        opacity: 0.5
    },
    label: {
        fill: green[500]
    }
};

const baselineDownStyle = {
    line: {
        stroke: red[500],
        strokeWidth: 1,
        opacity: 0.5
    },
    label: {
        fill: red[500]
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
    },
    button: {
        marginTop: theme.spacing.unit,
        marginBottom: theme.spacing.unit
    },
    progress: {
        marginTop: theme.spacing.unit,
        marginBottom: theme.spacing.unit * 2
    },
    note: {
        display: 'inline-flex',
		alignItems: 'center'
	},
	notePanel: {
        backgroundColor: yellow[50],
        marginBottom: theme.spacing.unit
    },
    panelDetails: {
        paddingTop: 0
    },
    detailsText: {
        fontWeight: 400,
        lineHeight: '1.5em',
        color: grey[900]
    },
    divider: {
        marginTop: theme.spacing.unit,
        marginBottom: theme.spacing.unit
    },
    leftgap: {
        paddingLeft: theme.spacing.unit * 2
    }
});

const MIN = 0;
let duration = 20, cycle = 5, MAX = 5;
const normalise = value => (value - MIN) * 100 / (MAX - MIN);

class TestCube extends Component {
    constructor(props) {
        super(props);
        this.state = {
            relaySeries: null,
            inputSeries: null,
            completed: 0,
            buffer: 0,
            started: false,
            finished: false,
            expanded: null
        };
    }

    startTest = () => {
        dataProvider(CREATE, 'test', {
            data: { cubeID: this.props.cubeID, duration: duration, cycle: cycle }
        });

        this.timer = setInterval(this.progress, 5000);

        this.setState({
            relaySeries: null,
            inputSeries: null,
            completed: 0,
            buffer: 0,
            started: true,
            finished: false
        });
    };

    progress = () => {
        const { completed, finished } = this.state;

        if (finished) {
            clearInterval(this.timer);
            this.setState({
                completed: 0,
                buffer: 0,
                started: false,
                finished: false
            });
        }

        dataProvider(GET_ONE, 'test', {
            id: this.props.cubeID
        })
        .then(response => response.data)
        .then(data => {
            if (data) {
                let count = data.length;
                if (count === 0) {
                    const diff = Math.random() * 2;
                    const diff2 = Math.random() * 2;
                    this.setState({ 
                        completed: completed + diff, 
                        buffer: completed + diff + diff2 
                    });
                } else {
                    const relays = [];
                    const inputs = [];

                    data.map(item => {
                        let time = moment.utc(item.timestamp);
                        let local_time = time.local().valueOf();
                        relays.push([local_time, item.r]);
                        inputs.push([local_time, item.i]);
                        return null;
                    })

                    const relaySeries = new TimeSeries({
                        name: 'relay',
                        columns: ['time', 'value'],
                        points: relays
                    });
                
                    const inputSeries = new TimeSeries({
                        name: 'input',
                        columns: ['time', 'value'],
                        points: inputs
                    });

                    this.setState({
                        relaySeries,
                        inputSeries,
                        completed: count,
                        buffer: (count % 2) === 0 ? count + 1 : count,
                        finished: count === (duration / 5) + 1
                    });
                }
            }
        });
    };

    componentDidMount() {
        setTimeout(() => {
            window.dispatchEvent(new Event('resize'));
        }, 0);
    }
    
    componentWillUnmount() {
        clearInterval(this.timer);
    }

    handleNote = panel => (event, expanded) => {
		this.setState({
			expanded: expanded ? panel : false,
		});
	};

    handleDuration = (data) => {
        duration = data;
        MAX = (duration / 5) + 1;
    };

    handleCycle = (data) => {
        cycle = data;
    };

    render() {
        const { classes } = this.props;
        const { relaySeries, inputSeries, completed, buffer, started, finished, expanded } = this.state;

        const beginTime = moment();
        const endTime = moment().add(5, 'm');
        let timeRange = new TimeRange(beginTime, endTime);

        const relayCharts = [
            <Baseline
                key='r1'
                axis='relay'
                value={0.99}
                label='ON'
                position='right'
                style={ baselineUpStyle }
            />,
            <Baseline
                key='r2'
                axis='relay'
                value={0.01}
                label='OFF'
                position='right'
                style={ baselineDownStyle }
            />
        ];
        if (relaySeries) {
            timeRange = relaySeries.range();
            relayCharts.push(
                <LineChart
                    key='r3'
                    axis='relay'
                    series={ relaySeries }
                    interpolation='curveStep'
                    style={ relayStyle }
                />
            );
        }

        const inputCharts = [
            <Baseline
                key='i1'
                axis='input'
                value={0.99}
                label='HIGH'
                position='right'
                style={ baselineUpStyle }
            />,
            <Baseline
                key='i2'
                axis='input'
                value={0.01}
                label='LOW'
                position='right'
                style={ baselineDownStyle }
            />
        ];
        if (inputSeries) {
            inputCharts.push(
                <LineChart
                    key='i3'
                    axis='input'
                    series={ inputSeries }
                    interpolation='curveStep'
                    style={ inputStyle }
                />
            );
        }

        const progress = started ? 
            <LinearProgress className={classes.progress} variant='buffer' value={normalise(completed)} valueBuffer={normalise(buffer)} /> 
            : 
            <LinearProgress className={classes.progress} variant='determinate' value={0}/>
            ;

        const message = started ?
            <Typography className={classes.leftgap} variant='subheading'>
                <i style={{color: green[500]}}>Test in process...</i>
            </Typography>
            :
                finished ? 
                <Typography className={classes.leftgap} variant='subheading'>
                    <i style={{color: red[500]}}>Test is finished!</i>
                </Typography>
                :
                    null
                ;
            ;

        return (
            <div>
                <ExpansionPanel className={classes.notePanel} expanded={expanded === 'test_note'} onChange={this.handleNote('test_note')}>
                    <ExpansionPanelSummary expandIcon={<ExpandMoreIcon />}>
                        <i style={{color: red[800]}}>Note!</i>
                        Testing process will take from 2 to 5 minutes (depends on network speed). Press [START...] to run.
                    </ExpansionPanelSummary>
                    <ExpansionPanelDetails className={classes.panelDetails}>
                        <div className={classes.detailsText}>
                            RELAY will cycle for <SelectDuration onChange={this.handleDuration} /> with <SelectCycle onChange={this.handleCycle}/> <b style={{color: green[400]}}>ON</b>/<b style={{color: red[400]}}>OFF</b> intervals.
                        <Divider className={classes.divider} />
                            Connect <b style={{color: blue[400]}}>INPUT (#5)</b> to <b style={{color: blue[400]}}>NC - Normal Close (#8)</b> RELAY's contact
                            <br/>
                            Connect <b style={{color: red[400]}}>+12V (#3)</b> to <b style={{color: red[400]}}>COM - Common (#7)</b> RELAY's contact.
                        <Divider className={classes.divider} />
                            Input will reflect RELAY's switches and show <b style={{color: green[400]}}>HIGH</b>/<b style={{color: red[400]}}>LOW</b> level. 
                        </div>
                    </ExpansionPanelDetails>
                </ExpansionPanel>

                <span className={classes.title}>
                    <Button 
                        className={classes.button} 
                        variant='contained'
                        color='primary' 
                        label='Start...' 
                        onClick={this.startTest}
                        disabled={started} 
                    >
                        <StartTestIcon />
                    </Button>
                    
                    {message}
                </span>

                {progress}

                <Resizable>
                    <ChartContainer
                        timeRange={timeRange} 
                        format='%H:%M:%S'
                    >
                        <ChartRow height='100'>
                            <YAxis
                                id='relay'
                                label='RELAY'
                                min={0} max={1}
                                tickCount={2}
                                format=',.0f'
                                type='linear'
                                style={ relayStyle }
                            />
                            <Charts>
                                {relayCharts}
                            </Charts>
                        </ChartRow>
                        <ChartRow height='100'>
                            <YAxis
                                id='input'
                                label='INPUT'
                                min={0} max={1}
                                tickCount={2}
                                format=',.0f'
                                type='linear'
                                style={ inputStyle }
                            />
                            <Charts>
                                {inputCharts}
                             </Charts>
                        </ChartRow>
                    </ChartContainer>
                </Resizable>
            </div>
        );
    }
}

TestCube.propTypes = {
    cubeID: PropTypes.string
};

export default withStyles(styles)(TestCube);
