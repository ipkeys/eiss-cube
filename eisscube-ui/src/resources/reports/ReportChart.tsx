import { useEffect, useState, useCallback } from 'react';
import { useRecordContext } from 'react-admin';
import { dataProvider } from '../../providers';
import {
	BarChart,
	Bar,
	XAxis,
	YAxis,
	CartesianGrid,
	Legend,
	Tooltip,
	Brush,
	ResponsiveContainer
} from 'recharts';

import { Box, Toolbar, IconButton } from '@mui/material';

import PrevIcon from '@mui/icons-material/ArrowBackIosNew';
import NextIcon from '@mui/icons-material/ArrowForwardIos';

import { teal } from '@mui/material/colors';
import SelectAggregation from './fields/SelectAggregation';
import SelectDateRange from './fields/SelectDateRange';
import DayPicker from './fields/DayPicker';
import WeekPicker from './fields/WeekPicker';
import MonthPicker from './fields/MonthPicker';
import YearPicker from './fields/YearPicker';
import moment from 'moment';

const TimestampTick = (props: any) => {
	const { x, y, payload } = props;

	return (
		<g transform={`translate(${x},${y})`}>
			<text x={0} y={0} dy={16} textAnchor='middle' >
				{moment(payload.value).format('HH:mm')}
			</text>
		</g>
	);
}

const ReportChart = (props: any) => {
    const record = useRecordContext(props);

	const current_moment = moment([moment().year(), moment().month(), moment().date(), 0, 0, 0]);
	const from_moment = moment(current_moment);
	const to_moment = moment(from_moment).add(1440, 'minutes');
	const utcOffset = moment().utcOffset();

	const [future, setFuture] = useState(true);
	const [from, setFrom] = useState(from_moment);
	const [to, setTo] = useState(to_moment);

	const [aggregation, setAggregation] = useState('1h');
	const [daterange, setDaterange] = useState('d');
	const [factor, setFactor] = useState(1000);
	const [unit, setUnit] = useState('kWh');
	const [meter, setMeter] = useState('e');
	const [load, setLoad] = useState(1);
	const [watch, setWatch] = useState('r');

	const [dataForCharts, setDataForCharts] = useState<any[]>([]);

	const getData = useCallback(() => {
		dataProvider.usage('meters', {
			data: {
				cubeID: record.cubeID,
				type: record.type,
				from,
				to,
				utcOffset,
				aggregation,
				load,
				watch,
				factor,
				unit,
				meter
			}
		})
		.then(response => response.data.usage)
		.then(usage => {
			const results = [] as any[];

			usage.forEach((item: any) => {
				let time = moment.utc(item.t);
				let local_time = time.local().valueOf();
				results.push({
					t: local_time,
					v: item.v
				});
			})

			setDataForCharts(results);
		});
	}, [record, aggregation, factor, from, to, unit, load, meter, watch, utcOffset]);

	useEffect(() => {
		dataProvider.getOne(`setup`, {
			id: record.cubeID
		})
		.then(response => response.data)
		.then(data => {
			// @ts-ignore
			const input = data.input;

			const new_watch = input && input.signal === 'c' && input.watch;
			const new_load = input && input.signal === 'c' && input.load;

			const new_factor = input && input.signal === 'p' && input.factor;

			const new_meter = input && input.meter;
			let new_unit = input && input.unit;

			if (new_meter === 'e') { // remove last 'h' from Wh, kWh or MWh
				new_unit = new_unit.replace(/h$/, '');
			}

			if (record.type === 'p' && new_factor && new_unit && new_meter) {
				setMeter(new_meter);
				setFactor(new_factor);
				setUnit(new_unit);
			}

			if (record.type === 'c' && new_watch && new_load) {
				setMeter(new_meter);
				setWatch(new_watch);
				setLoad(new_load);
				setUnit(new_unit);
			}
		});
	}, [record]);

	useEffect(() => {
		getData();
	}, [getData]);

	const handleAggregation = (new_aggregation: any) => {
		setAggregation(new_aggregation);
	}

	const handleDateRange = (new_daterange: any) => {
		let new_from, new_to, new_aggregation;

		switch(new_daterange) {
			case 'w':
				new_from = moment(from).startOf('week');
				new_to = moment(new_from).add(1, 'weeks');
				new_aggregation = '1h';
				break;
			case 'm':
				new_from = moment(from).startOf('month');
				new_to = moment(new_from).add(1, 'months');
				new_aggregation = '1h';
				break;
			case 'y':
				new_from = moment(from).startOf('year');
				new_to = moment(new_from).add(1, 'years');
				new_aggregation = '1h';
				break;
			case 'd':
			default:
				new_from = moment(from).startOf('day');
				new_to = moment(new_from).add(1, 'days');
				new_aggregation = '1h';
				break;
		}

		setFuture(current_moment.diff(new_from, 'days') === 0);
		setFrom(new_from);
		setTo(new_to);
		setDaterange(new_daterange);
		setAggregation(new_aggregation);
	}

	const handleDateChange = (from_param: any) => {
		let new_from, new_to;
		switch(daterange) {
			case 'w':
				new_from = moment(from_param).startOf('week');
				new_to = moment(new_from).add(1, 'weeks');
				break;
			case 'm':
				new_from = moment(from_param).startOf('month');
				new_to = moment(new_from).add(1, 'months');
				break;
			case 'y':
				new_from = moment(from_param).startOf('year');
				new_to = moment(new_from).add(1, 'years');
				break;
			case 'd':
			default:
				new_from = moment(from_param).startOf('day');
				new_to = moment(new_from).add(1, 'days');
				break;
		}

		setFuture(current_moment.diff(new_from, 'days') === 0);
		setFrom(new_from);
		setTo(new_to);
	}

	const handleStepBack = (from_param: any) => {
		let new_from, new_to;
		switch(daterange) {
			case 'w':
				new_from = moment(from_param).subtract(1, 'weeks');
				new_to = moment(new_from).add(1, 'weeks');
				break;
			case 'm':
				new_from = moment(from_param).subtract(1, 'months');
				new_to = moment(new_from).add(1, 'months');
				break;
			case 'y':
				new_from = moment(from_param).subtract(1, 'years');
				new_to = moment(new_from).add(1, 'years');
				break;
			case 'd':
			default:
				new_from = moment(from_param).subtract(1, 'days');
				new_to = moment(new_from).add(1, 'days');
				break;
		}

		setFuture(current_moment.diff(new_from, 'days') === 0);
		setFrom(new_from);
		setTo(new_to);
	}

	const handleStepForward = (from_param: any) => {
		let new_from, new_to;
		switch(daterange) {
			case 'w':
				new_from = moment(from_param).add(1, 'weeks');
				new_to = moment(new_from).add(1, 'weeks');
				break;
			case 'm':
				new_from = moment(from_param).add(1, 'months');
				new_to = moment(new_from).add(1, 'months');
				break;
			case 'y':
				new_from = moment(from_param).add(1, 'years');
				new_to = moment(new_from).add(1, 'years');
				break;
			case 'd':
			default:
				new_from = moment(from_param).add(1, 'days');
				new_to = moment(new_from).add(1, 'days');
				break;
		}

		setFuture(current_moment.diff(new_from, 'days') === 0);
		setFrom(new_from);
		setTo(new_to);
	}

	let renderCalendar;
	switch(daterange) {
		case 'w':
			renderCalendar = <WeekPicker date={from} onChange={handleDateChange} />;
			break;
		case 'm':
			renderCalendar = <MonthPicker date={from} onChange={handleDateChange} />;
			break;
		case 'y':
			renderCalendar = <YearPicker date={from} onChange={handleDateChange} />;
			break;
		case 'd':
		default:
			renderCalendar = <DayPicker date={from} onChange={handleDateChange} />;
			break;
	}

	return (
		<Box>
			<Toolbar sx={{flex: 1, display: 'flex', justifyContent: 'flex-end'}} >
				<SelectDateRange label='Date range' daterange={daterange} onChange={handleDateRange} />
				<SelectAggregation label='Aggregate by' aggregation={aggregation} daterange={daterange} onChange={handleAggregation} />
				<Box>
					<IconButton sx={{mt: 1.5}} onClick={() => handleStepBack(from)} >
						<PrevIcon />
					</IconButton>

					{renderCalendar}

					<IconButton sx={{mt: 1.5}} disabled={future} onClick={() => handleStepForward(from)} >
						<NextIcon />
					</IconButton>
				</Box>
			</Toolbar>

			<Box sx={{mt: 1, mb: 1}}>
				<ResponsiveContainer width='100%' height={500}>
					<BarChart data={dataForCharts} >
						<CartesianGrid strokeDasharray='3 3' />
						<XAxis dataKey='t' tick={<TimestampTick />} />
						<YAxis domain={[0, 'dataMax + 100']}/>
						<Tooltip labelFormatter={(value: any) => moment(value).format('MM/DD/YYYY, HH:mm')} formatter={(value: any) => [`${value} ${unit}`, 'Value']} />
						<Legend align='left' verticalAlign='top' height={40} formatter={() => `${unit}`} />
						<Brush dataKey='t' height={30} stroke={teal[300]} tickFormatter={(value: any) => moment(value).format('MM/DD/YYYY, HH:mm')} />
						<Bar dataKey='v' fill={teal[500]} minPointSize={2} />
					</BarChart>
				</ResponsiveContainer>
			</Box>
		</Box>
	);
}

export default ReportChart;
