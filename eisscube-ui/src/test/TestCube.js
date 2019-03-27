import React, { Component } from 'react';
import PropTypes from 'prop-types';
import { withStyles } from '@material-ui/core/styles';
import moment from 'moment';
import { Button, CREATE, GET_ONE } from 'react-admin';
import { dataProvider } from '../App';
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
import { green, red, blue, amber, yellow } from '@material-ui/core/colors';

const relayStyle = {
    value: { 
        normal: {
            stroke: blue[800], 
            fill: "none", 
            strokeWidth: 2
        } 
    }
};

const inputStyle = {
    value: { 
        normal: {
            stroke: amber[800], 
            fill: "none", 
            strokeWidth: 2
        } 
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
    divider: {
        marginTop: theme.spacing.unit,
        marginBottom: theme.spacing.unit
    },
    leftgap: {
        paddingLeft: theme.spacing.unit * 2
    }
});

const MIN = 0;
const MAX = 25;
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
            data: {cubeID: this.props.cubeID}
        });

        this.timer = setInterval(this.progress, 5000);

        this.setState({
            relaySeries: null,
            inputSeries: null,
            started: true
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
                    this.setState({ completed: completed + diff, buffer: completed + diff + diff2 });
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
                        name: "relay",
                        columns: ["time", "value"],
                        points: relays
                    });
                
                    const inputSeries = new TimeSeries({
                        name: "input",
                        columns: ["time", "value"],
                        points: inputs
                    });

                    this.setState({
                        relaySeries,
                        inputSeries,
                        completed: count,
                        buffer: count + 1,
                        finished: count === 25
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

    render() {
        const { classes } = this.props;
        const { relaySeries, inputSeries, completed, buffer, started, finished, expanded } = this.state;

        const beginTime = moment();
        const endTime = moment().add(5, 'm');
        let timeRange = new TimeRange(beginTime, endTime);

        const relayCharts = [
            <Baseline
                axis="relay"
                value={0.99}
                label="ON"
                position="right"
                style={ baselineUpStyle }
            />,
            <Baseline
                axis="relay"
                value={0.01}
                label="OFF"
                position="right"
                style={ baselineDownStyle }
            />
        ];
        if (relaySeries) {
            timeRange = relaySeries.range();
            relayCharts.push(
                <LineChart
                    axis="relay"
                    series={ relaySeries }
                    interpolation="curveStep"
                    style={ relayStyle }
                />
            );
        }

        const inputCharts = [
            <Baseline
                axis="input"
                value={0.99}
                label="HIGH"
                position="right"
                style={ baselineUpStyle }
            />,
            <Baseline
                axis="input"
                value={0.01}
                label="LOW"
                position="right"
                style={ baselineDownStyle }
            />
        ];
        if (inputSeries) {
            inputCharts.push(
                <LineChart
                    axis="input"
                    series={ inputSeries }
                    interpolation="curveStep"
                    style={ inputStyle }
                />
            );
        }

        const progress = started ? 
            <LinearProgress className={classes.progress} variant="buffer" value={normalise(completed)} valueBuffer={normalise(buffer)} /> 
            : 
            <LinearProgress className={classes.progress} variant="determinate" value={0}/>
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
                        <i style={{color: red[500]}}>Note!</i>
                        Testing process will take from 2 to 5 minutes (depends on network speed). Press [START...] to run.
                    </ExpansionPanelSummary>
                    <ExpansionPanelDetails>
                        <Typography paragraph>
                        RELAY will cycle during <b>2 minutes</b> with <b style={{color: green[500]}}>15 seconds ON</b> and <b style={{color: red[500]}}>15 seconds OFF</b>.
                        <Divider className={classes.divider} />
                        Connect <b style={{color: blue[500]}}>INPUT (#5)</b> to <b style={{color: blue[500]}}>NC - Normal Close (#8)</b> RELAY's contact
                        <br/>
                        Connect <b style={{color: red[500]}}>+12V (#3)</b> to <b style={{color: red[500]}}>COM - Common (#7)</b> RELAY's contact.
                        <Divider className={classes.divider} />
                        Input will reflect RELAY's switches and show <b style={{color: green[500]}}>HIGH</b> and <b style={{color: red[500]}}>LOW</b> level. 
                        </Typography>
                    </ExpansionPanelDetails>
                </ExpansionPanel>

                <span className={classes.title}>
                    <Button 
                        className={classes.button} 
                        variant="contained" 
                        color="primary" 
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
                        format="%H:%M:%S"
                    >
                        <ChartRow height="100">
                            <YAxis
                                id="relay"
                                label="RELAY"
                                min={0} max={1}
                                tickCount={2}
                                format=",.0f"
                                type="linear"
                            />
                            <Charts>
                                {relayCharts}
                            </Charts>
                        </ChartRow>
                        <ChartRow height="100">
                            <YAxis
                                id="input"
                                label="INPUT"
                                min={0} max={1}
                                tickCount={2}
                                format=",.0f"
                                type="linear"
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
