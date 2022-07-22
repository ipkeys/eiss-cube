import { useEffect, useState } from 'react';
import moment from 'moment';
import { Button, useDataProvider, useRecordContext } from 'react-admin';
import {
	Box,
	Divider,
	Typography,
	LinearProgress,
	Accordion,
	AccordionDetails,
	AccordionSummary
} from '@mui/material';
import {
	LineChart,
	Line,
	XAxis,
	YAxis,
	CartesianGrid,
	Legend,
	ReferenceLine
} from 'recharts';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import StartTestIcon from '@mui/icons-material/PlayCircleOutline';
import { green, red, blue, yellow } from '@mui/material/colors';
import SelectDuration from './SelectDuration';
import SelectCycle from './SelectCycle';

let duration = 60, cycle = 10; // default
let STEP = 0;
let STEPS = duration / 5; // 5 seconds steps for linear progress

const TestCube = () => {
	const record = useRecordContext();
	const [completed, setCompleted] = useState(0);
	const [buffer, setBuffer] = useState(0);
	const [started, setStarted] = useState(false);
	const [finished, setFinished] = useState(false);
	const [expanded, setExpanded] = useState('');
	const [dataForCharts, setDataForCharts] = useState<any[]>([]);
	const dataProvider = useDataProvider();

	const cubeID = record.id;

	const normalise = (value: any) => value * 100 / STEPS;

	useEffect(() => {
		let timer = setInterval(() => {
			if (started) {
				progress();
			}
			if (finished) {
				clearInterval(timer);
			}
		}, 5000);

		return () => {
			clearInterval(timer);
		};
	});

	if (!record) return null;

	const startTest = () => {
		STEP = 0;

		dataProvider.create('test', {
			data: { cubeID, duration, cycle }
		});

		setCompleted(0);
		setBuffer(0);
		setStarted(true);
		setFinished(false);
	};

	const progress = () => {
		if (finished) {
			setCompleted(0);
			setBuffer(0);
			setStarted(false);
			setFinished(true);
		}

		dataProvider.test('test', {
			data: {id: cubeID}
		})
		.then((data: any) => {
			const arr = data.data;
			if (arr) {
				let count = arr.length;

				if (count === 0) { // no data yet
					const diff = Math.random() * 2;
					const diff2 = Math.random() * 2;
					setCompleted(completed + diff);
					setBuffer(completed + diff + diff2);
				} else {
					STEP++; // count steps

					const results = [] as any[];

					arr.forEach((item: any) => {
						let time = moment.utc(item.timestamp);
						let local_time = time.local().valueOf();
						results.push({
							t: moment(local_time).format('HH:mm:ss'),
							r: item.r,
							i: item.i
						});
					})

					setDataForCharts(results);
					setCompleted(STEP);
					setBuffer((STEP % 2) === 0 ? (STEP + 1) : STEP);
					setFinished(STEP === STEPS);
				}
			}
		});
	};

	const handleNote = (panel: string) => (_event: any, exp: boolean) => {
		setExpanded(exp ? panel : '');
	};

	const handleDuration = (data: number) => {
		duration = data;
		STEPS = duration / 5;
	};

	const handleCycle = (data: number) => {
		cycle = data;
	};

	const linearProgress = started ?
		<LinearProgress sx={{mt: 2, mb: 2}} variant='buffer' value={normalise(completed)} valueBuffer={normalise(buffer)} />
		:
		<LinearProgress sx={{mt: 2, mb: 2}} variant='determinate' value={0}/>
		;

	let message = null;

	if (started) {
		message =
		<Typography sx={{pf: 2, ml: 1}} variant='body2'>
			<i style={{color: green[500]}}>Test in process...</i>
		</Typography>
	}

	if (finished) {
		message =
		<Typography sx={{pf: 2, ml: 1}} variant='body2'>
			<i style={{color: red[500]}}>Test is finished!</i>
		</Typography>
	}

	return (
		<Box>
			<Accordion sx={{backgroundColor: yellow[50], mb: 2}} expanded={expanded === 'test_note'} onChange={handleNote('test_note')}>
				<AccordionSummary expandIcon={<ExpandMoreIcon />}>
					<i style={{color: red[500], marginRight: '1em'}}>Note!</i>
					Testing process will take up to 5 minutes (depends on network speed). Press [START...] to run.
				</AccordionSummary>
					<AccordionDetails>
						<Box sx={{display: 'inline-flex', alignItems: 'center'}}>
							RELAY will cycle for <SelectDuration onChange={handleDuration} /> with <SelectCycle onChange={handleCycle}/> <b style={{color: green[400]}}>ON</b>/<b style={{color: red[400]}}>OFF</b>&nbsp;intervals.
						</Box>

						<Divider sx={{mt: 1, mb: 1}}/>

						Connect <b style={{color: blue[400]}}>INPUT (#5)</b> to <b style={{color: blue[400]}}>NC - Normal Close (#8)</b> of RELAY's contact
						<br/>
						Connect <b style={{color: red[400]}}>+12V (#3)</b> to <b style={{color: red[400]}}>COM - Common (#7)</b> of RELAY's contact

						<Divider sx={{mt: 1, mb: 1}}/>

						Input will reflect RELAY's switches and show <b style={{color: green[400]}}>HIGH</b>/<b style={{color: red[400]}}>LOW</b> level.
					</AccordionDetails>
			</Accordion>

			<Box sx={{display: 'inline-flex', alignItems: 'center'}} >
				<Button
					variant='contained'
					color='primary'
					label='Start...'
					onClick={startTest}
					disabled={started}
				>
					<StartTestIcon />
				</Button>

				{message}

			</Box>

			{linearProgress}

			<LineChart
				width={850}
				height={150}
				data={dataForCharts}
				margin={{top: 5, right: 30, left: 5, bottom: 5}}
			>
				<CartesianGrid strokeDasharray='3 3' horizontal={false} />
				<ReferenceLine y={1} stroke='green' strokeDasharray='3 3' />
				<ReferenceLine y={0} stroke='red' strokeDasharray='3 3' />
				<XAxis dataKey='t' domain={[moment().format('HH:mm:ss'), moment().add('5m').format('HH:mm:ss')]} />
				<YAxis type='number' domain={[0, 1]} padding={{top: 10, bottom: 10}} interval={3} tickFormatter={n => (n === 0) ? 'OFF' : 'ON'} />
				<Legend align='left' verticalAlign='top' iconType='circle' />
				<Line dataKey='r' type='step' name='Relay' stroke={blue.A700} strokeWidth={3} dot={false} isAnimationActive={false} />
			</LineChart>

			<LineChart
				width={850}
				height={150}
				data={dataForCharts}
				margin={{top: 5, right: 30, left: 5, bottom: 5}}
			>
				<CartesianGrid strokeDasharray='3 3' horizontal={false} />
				<ReferenceLine y={1} stroke='green' strokeDasharray='3 3' />
				<ReferenceLine y={0} stroke='red' strokeDasharray='3 3' />
				<XAxis dataKey='t' domain={[moment().format('HH:mm:ss'), moment().add('5m').format('HH:mm:ss')]} />
				<YAxis type='number' domain={[0, 1]} padding={{top: 10, bottom: 10}} interval={3} tickFormatter={n => (n === 0) ? 'OFF' : 'ON'} />
				<Legend align='left' verticalAlign='top' iconType='circle' />
				<Line dataKey='i' type='step' name='Input' stroke={yellow.A700} strokeWidth={3} dot={false} isAnimationActive={false} />
			</LineChart>
		</Box>
	);
}

export default TestCube;

/*
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

*/