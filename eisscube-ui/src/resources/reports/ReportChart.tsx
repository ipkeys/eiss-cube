import React, { useEffect, useState, useCallback } from 'react';
import { makeStyles, Theme } from '@material-ui/core/styles';

import { useRecordContext, Button } from 'react-admin';
import { dataProvider } from '../../providers';

import { TimeSeries, Index } from "pondjs";
import { Charts, ChartContainer, ChartRow, YAxis, AreaChart, BarChart, Resizable, Brush } from "react-timeseries-charts";
import { Toolbar, Typography } from '@material-ui/core';
import PrevIcon from '@material-ui/icons/ArrowBackIos';
import NextIcon from '@material-ui/icons/ArrowForwardIos';
import { grey, red, teal } from '@material-ui/core/colors';
import { DividerField } from '../common';
import SelectAggregation from './fields/SelectAggregation';
import SelectDateRange from './fields/SelectDateRange';
import DayPicker from './fields/DayPicker';
import WeekPicker from './fields/WeekPicker';
import MonthPicker from './fields/MonthPicker';
import YearPicker from './fields/YearPicker';
import moment from 'moment';
import MomentUtils from '@date-io/moment';
import { MuiPickersUtilsProvider } from '@material-ui/pickers';

const chartstyle = {
	value: {
		normal: { fill: teal[400], opacity: 0.9, },
		highlighted: { fill: teal[400], opacity: 1.0, },
		selected: { fill: teal[400], opacity: 1.2, },
		muted: { fill: teal[400], opacity: 0.7 }
	},
	brush: {
		line: {
            normal: {stroke: teal[400], fill: "none", strokeWidth: 1},
            highlighted: {stroke: teal[400], fill: "none", strokeWidth: 1},
            selected: {stroke: teal[400], fill: "none", strokeWidth: 1},
            muted: {stroke: teal[400], fill: "none", opacity: 0.4, strokeWidth: 1}
        },
        area: {
            normal: {fill: teal[100], stroke: "none", opacity: 0.75},
            highlighted: {fill: teal[100], stroke: "none", opacity: 0.75},
            selected: {fill: teal[100], stroke: "none", opacity: 0.75},
            muted: {fill: teal[100], stroke: "none", opacity: 0.25}
        }
	},
	axis: {
		values: {
			fill: 'rgba(0, 0, 0, 0.87)',
			'font-size': 12
		}
	},
	infoBox: {
		markerStyle: {
			fill: red[500]
		},
		stemStyle : {
			stroke: red[500]
		},
		box: { 
			stroke: red[500], 
			fill: red[50], 
			opacity: 1
		} 
	},
	tracker: { 
		line: { 
			stroke: red[500] 
		} 
	}
};

const useStyles = makeStyles((theme: Theme) => ({ 
	toolbar: {
		paddingLeft: 0,
		paddingRight: 0,
		flex: 1,
		display: 'flex',
		justifyContent: 'flex-end'
	},
	calendar: {
		alignContent: 'center',
		paddingBottom: theme.spacing(1)
	},
	prevbtn: {
		marginTop: theme.spacing(1.5),
		marginRight: theme.spacing(1)
	},
	nextbtn: {
		marginLeft: theme.spacing(1),
		marginTop: theme.spacing(1.5)
	},
	date: {
		marginTop: theme.spacing(1),
		width: theme.spacing(11)
	},
	info: {
		marginTop: theme.spacing(2),
		display: 'inline-flex',
		alignItems: 'center'
	},
	info_datetime: {
		width: theme.spacing(20)
	},
	info_power: {
		width: theme.spacing(20)
	},
	chart: {
		marginTop: theme.spacing(1)
	}
}));


const ReportChart = (props: any) => {
	const classes = useStyles();
    const record = useRecordContext(props);

	const current_moment = moment([moment().year(), moment().month(), moment().date(), 0, 0, 0]);
	const from_moment = moment(current_moment);
	const to_moment = moment(from_moment).add(1440, 'minutes');
	const utcOffset = moment().utcOffset();
	const minDuration = 3600000;

	const [ready, setReady] = useState(false);
	const [future, setFuture] = useState(true);
	const [from, setFrom] = useState(from_moment);
	const [to, setTo] = useState(to_moment);

	const [series, setSeries] = useState<TimeSeries | null>(null);
	const [brushseries, setBrushseries] = useState<TimeSeries | null>(null);

	const [timerange, setTimerange] = useState(null);
	const [brushrange, setBrushrange] = useState(null);

	const [tracker, setTracker] = useState(null);
	const [selection, setSelection] = useState(null);
	const [highlight, setHighlight] = useState(null);
	
	const [aggregation, setAggregation] = useState('1h');
	const [daterange, setDaterange] = useState('d');
	const [factor, setFactor] = useState(1000);
	const [unit, setUnit] = useState('kWh');
	const [meter, setMeter] = useState('e');
	const [load, setLoad] = useState(1);
	const [watch, setWatch] = useState('r');

	const [minTime, setMinTime] = useState(null);
	const [maxTime, setMaxTime] = useState(null);

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
			const points = [];
			const brushPoints = [];

			if (usage.length === 0) {
				setReady(false);
			} else {
				for (const element of usage) {
					const time = moment(element.t);
					const value = element.v;
					// @ts-ignore
					const index = Index.getIndexString(aggregation, time); 

					points.push([index, value]);					
					brushPoints.push([index, value]);
				}

				const new_series = new TimeSeries({
					name: "data",
					columns: ["index", "value"],
					points: points
				});

				const new_brushSeries = new TimeSeries({
					name: "brush",
					columns: ["index", "brush"],
					points: brushPoints
				});

				// @ts-ignore
				const initialRange = new_series && new_series.range();
				const newMinTime = initialRange && initialRange.begin();
				const newMaxTime = initialRange && initialRange.end();
				
				setReady(true);
				setSeries(new_series);
				setBrushseries(new_brushSeries);
				setTimerange(initialRange);
				setBrushrange(initialRange);
				setMinTime(newMinTime);
				setMaxTime(newMaxTime);
			}
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

			if (new_meter === 'e') { // remove lah 'h' from Wh, kWh or MWh
				new_unit = new_unit.replace("h", "");
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

	const handleTrackerChanged = (new_tracker: any) => setTracker(new_tracker);

	const handleTimeRangeChange = (new_timerange: any) => {
		if (new_timerange) {
			setTimerange(new_timerange);
			setBrushrange(new_timerange);
		} else {
			// @ts-ignore
			setTimerange(brushseries.range());
			setBrushrange(null);
		}
	}

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

	let selectedDate = '--';
	let selectedValue = '--';
	let renderCharts = <div className={classes.chart}>Loading.....</div>;

	if (ready && series && timerange) {
		// @ts-ignore
		selectedDate = selection && `${moment(selection.event.index().begin()).format('MM/DD/YYYY, HH:mm')}`;

		// @ts-ignore
		selectedValue = selection && `${selection.event.value(selection.column)} ${unit}`;

		let infoValues = [] as any[];

		if (highlight) {
			// @ts-ignore
			const energyText = `${highlight.event.get(highlight.column)} ${unit}`;
			infoValues = [{ label: 'Value', value: energyText }];
		}

		const seriesCropped = series.crop(timerange);
		const y_axis_label = `${unit}`;

		renderCharts =
		<div className={classes.chart}>
			<Resizable>
				<ChartContainer
					format='%H:%M'
					utc={true}
					timeRange={timerange}
					timeAxisStyle={chartstyle.axis}
					maxTime={maxTime}
					minTime={minTime}
					minDuration={minDuration}
					enablePanZoom
					onTimeRangeChanged={handleTimeRangeChange}
					onTrackerChanged={handleTrackerChanged}
				>
					<ChartRow height='400'>
						<YAxis
							id='axis'
							label={y_axis_label}
							labelOffset={-10}
							min={0}
							// @ts-ignore
							max={series.max() + series.max() * 0.1}
							width={60}
							valWidth={40}
							format=',.2f'
							type='linear'
							style={chartstyle.axis}
						/>
						<Charts>
							<BarChart
								axis='axis'
								style={chartstyle}
								columns={['value']}
								series={seriesCropped}
								minBarHeight={1}
								breakLine
								info={infoValues}
								infoWidth={100}
								infoOffsetY={5}
								infoTimeFormat={(index: any) => moment(index.begin()).format('MM/DD/YYYY, HH:mm')}
								infoStyle={chartstyle.infoBox}
								stemStyle={chartstyle.infoBox.stemStyle}
								markerStyle={chartstyle.infoBox.markerStyle}
								highlighted={highlight}
								onHighlightChange={(h: any) => setHighlight(h)}
								selected={selection}
								onSelectionChange={(s: any) => setSelection(s)}
							/>
						</Charts>
					</ChartRow>
				</ChartContainer>
			</Resizable>
			<Resizable>
				<ChartContainer
					format='%H:%M'
					utc={true}
					// @ts-ignore
					timeRange={brushseries.range()}
					timeAxisStyle={chartstyle.axis}
					trackerPosition={tracker}
					trackerStyle={chartstyle.tracker}
					maxTime={maxTime}
					minTime={minTime}
					minDuration={minDuration}
				>
					<ChartRow height='80'>
						<Brush
							timeRange={brushrange}
							allowSelectionClear
							onTimeRangeChanged={handleTimeRangeChange}
						/>
						<YAxis
							id='axis1'
							min={0}
							// @ts-ignore
							max={brushseries.max('brush') + brushseries.max('brush') * 0.1}
							width={60}
							valWidth={40}
							format = ',.2f'
							style={chartstyle.axis}		
						/>
						<Charts>
							<AreaChart
								axis='axis1'
								style={chartstyle}
								columns={{ up: ['brush'], down: [] }}
								series={brushseries}
							/>
						</Charts>
					</ChartRow>
				</ChartContainer>
			</Resizable>
		</div>
	}

	let renderCalendar;
	switch(daterange) {
		case 'w':
			renderCalendar = <WeekPicker date={from} onChange={handleDateChange} />
			break;
		case 'm':
			renderCalendar = <MonthPicker date={from} onChange={handleDateChange} />
			break;
		case 'y':
			renderCalendar = <YearPicker date={from} onChange={handleDateChange} />
			break;
		case 'd':
		default:
			renderCalendar = <DayPicker date={from} onChange={handleDateChange} />
			break;
	}
	
	return (
		<>
			<Toolbar className={classes.toolbar} >
				<Typography variant="body2" component='span'>
					Date range: <SelectDateRange daterange={daterange} onChange={handleDateRange} />
				</Typography>
				<Typography variant="body2" component='span'>
					Aggregate by: <SelectAggregation aggregation={aggregation} daterange={daterange} onChange={handleAggregation} />
				</Typography>
				<div className={classes.calendar}>
					<Button className={classes.prevbtn}
						variant='outlined'
						disabled={false}
						onClick={() => handleStepBack(from)}
					>
						<PrevIcon />
					</Button>
					<MuiPickersUtilsProvider utils={MomentUtils}>
						{renderCalendar}
					</MuiPickersUtilsProvider>
					<Button className={classes.nextbtn}
						variant='outlined'
						alignIcon='right'
						disabled={future}
						onClick={() => handleStepForward(from)}
					>
						<NextIcon />
					</Button>
				</div>
			</Toolbar>

			<DividerField />

			{ renderCharts }

			<DividerField />

			<div className={classes.info}>
				<Typography variant="body2" style={{ color: grey[500] }}>
					Date/Time:&nbsp;
				</Typography>
				<Typography variant="body2" className={classes.info_datetime} >
					{selectedDate}&nbsp;
				</Typography>
				<Typography variant="body2" style={{ color: grey[500] }}>
					Value:&nbsp;
				</Typography>
				<Typography variant="body2" className={classes.info_power} style={{ color: red[500] }}>
					{selectedValue}
				</Typography>
			</div>

		</>
	);
}

export default ReportChart;
