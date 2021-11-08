import { useEffect, useState } from 'react';
import { makeStyles, Theme } from '@material-ui/core/styles';
import moment from 'moment';
import { Button, useDataProvider } from 'react-admin';
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
import {
    Divider,
    Typography,
    LinearProgress,
    ExpansionPanel,
    ExpansionPanelDetails,
    ExpansionPanelSummary
} from '@material-ui/core';
import ExpandMoreIcon from '@material-ui/icons/ExpandMore';
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

const useStyles = makeStyles((theme: Theme) => ({ 
	btnPadding: {
        paddingRight: theme.spacing(1)
    },
    title: {
        display: 'inline-flex',
        alignItems: 'center',
    },
    content: {
        paddingLeft: theme.spacing(3),
        paddingRight: theme.spacing(3),
        paddingBottom: 0
    },
    button: {
        marginTop: theme.spacing(1),
        marginBottom: theme.spacing(1)
    },
    progress: {
        marginTop: theme.spacing(1),
        marginBottom: theme.spacing(2)
    },
    note: {
        display: 'inline-flex',
		alignItems: 'center'
	},
	notePanel: {
        backgroundColor: yellow[50],
        marginBottom: theme.spacing(1)
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
        marginTop: theme.spacing(1),
        marginBottom: theme.spacing(1)
    },
    leftgap: {
        paddingLeft: theme.spacing(2)
    }
}));


let duration = 60, cycle = 10; // default

let STEP = 0;
let STEPS = duration / 5; // 5 seconds steps for linear progress
const normalise = (value: any) => value * 100 / STEPS;

const TestCube = (props: any) => {
    const { cubeID } = props;
    const classes = useStyles();
    const [relaySeries, setRelaySeries] = useState<TimeSeries | null>(null);
    const [inputSeries, setInputSeries] = useState<TimeSeries | null>(null);
    const [completed, setCompleted] = useState(0);
    const [buffer, setBuffer] = useState(0);
    const [started, setStarted] = useState(false);
    const [finished, setFinished] = useState(false);
    const [expanded, setExpanded] = useState('');
    const dataProvider = useDataProvider();

    let timer: any = null;

    const startTest = () => {
        STEP = 0;
        
        dataProvider.create('test', {
            data: { cubeID, duration, cycle }
        });

        timer = setInterval(progress, 5000); // each 5 sec

        setRelaySeries(null);
        setInputSeries(null);
        setCompleted(0);
        setBuffer(0);
        setStarted(true);
        setFinished(false);
    };

    const progress = () => {
        if (finished) {
            clearInterval(timer);

            setCompleted(0);
            setBuffer(0);
            setStarted(false);
            setFinished(true);
        }

        dataProvider.getOne('test', {
            id: cubeID
        })
        .then(response => response.data)
        .then(data => {
            if (data) {
                let count = data.length;
                                
                if (count === 0) { // no data yet
                    const diff = Math.random() * 2;
                    const diff2 = Math.random() * 2;
                    setCompleted(completed + diff);
                    setBuffer(completed + diff + diff2);
                } else {
                    STEP++; // sount steps
                    console.log("STEP " + STEP + " of " + STEPS);

                    const relays = [] as any[];
                    const inputs = [] as any[];

                    data.map((item: any) => {
                        let time = moment.utc(item.timestamp);
                        let local_time = time.local().valueOf();
                        relays.push([local_time, item.r]);
                        inputs.push([local_time, item.i]);
                        return null;
                    })

                    const tsForRelay = new TimeSeries({
                        name: 'relay',
                        columns: ['time', 'value'],
                        points: relays
                    });
                
                    const tsForInput = new TimeSeries({
                        name: 'input',
                        columns: ['time', 'value'],
                        points: inputs
                    });

                    setRelaySeries(tsForRelay);
                    setInputSeries(tsForInput);
                    setCompleted(STEP);
                    setBuffer((STEP % 2) === 0 ? STEP + 1 : STEP);
                    setFinished(STEP === STEPS);
                }
            }
        });
    };

    useEffect(() => {
        setTimeout(() => {
            window.dispatchEvent(new Event('resize'));
        }, 0);
        return () => {
            clearInterval(timer);
        };
	});

    const handleNote = (panel: string) => (event: any, exp: boolean) => {
        setExpanded(exp ? panel : '');
	};

    const handleDuration = (data: number) => {
        duration = data;
        STEPS = duration / 5;
    };

    const handleCycle = (data: number) => {
        cycle = data;
    };

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
         // @ts-ignore
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

    const linearProgress = started ? 
        <LinearProgress className={classes.progress} variant='buffer' value={normalise(completed)} valueBuffer={normalise(buffer)} /> 
        : 
        <LinearProgress className={classes.progress} variant='determinate' value={0}/>
        ;

    let message = null;
        
    if (started) {
        message =
        <Typography className={classes.leftgap} variant='body2'>
            <i style={{color: green[500]}}>Test in process...</i>
        </Typography> 
    }

    if (finished) {
        message = 
        <Typography className={classes.leftgap} variant='body2'>
            <i style={{color: red[500]}}>Test is finished!</i>
        </Typography>
    }

    return (
        <div>
            <ExpansionPanel className={classes.notePanel} expanded={expanded === 'test_note'} onChange={handleNote('test_note')}>
                <ExpansionPanelSummary expandIcon={<ExpandMoreIcon />}>
                    <i style={{color: red[800], marginRight: '8px'}}>Note!</i>
                    Testing process will take up to 5 minutes (depends on network speed).<br/>Press [START...] to run.
                </ExpansionPanelSummary>
                    <ExpansionPanelDetails className={classes.panelDetails}>
                        <div className={classes.detailsText}>
                            RELAY will cycle for <SelectDuration onChange={handleDuration} /> with <SelectCycle onChange={handleCycle}/> <b style={{color: green[400]}}>ON</b>/<b style={{color: red[400]}}>OFF</b> intervals.
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
                    onClick={startTest}
                    disabled={started} 
                >
                    <StartTestIcon />
                </Button>
                
                {message}
            </span>

            {linearProgress}

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

export default TestCube;
