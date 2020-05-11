import React, { Component } from 'react';
import PropTypes from 'prop-types';
import { withStyles } from '@material-ui/core/styles';
import moment from 'moment';

import { GET_ONE, Button } from 'react-admin';
import { USAGE } from '../providers/dataProvider';
import { dataProvider } from '../providers';

import { TimeSeries, Index } from "pondjs";
import { Charts, ChartContainer, ChartRow, YAxis, AreaChart, BarChart, Resizable, Brush } from "react-timeseries-charts";

import { Toolbar } from '@material-ui/core';
import Typography from '@material-ui/core/Typography';
import PrevIcon from '@material-ui/icons/ArrowBackIos';
import NextIcon from '@material-ui/icons/ArrowForwardIos';

import { grey, red, teal } from '@material-ui/core/colors';
import DividerFiled from '../Layout/DividerField';
import SelectAggregation from './SelectAggregation';
import SelectDateRange from './SelectDateRange';
import MomentUtils from '@date-io/moment';
import { MuiPickersUtilsProvider } from '@material-ui/pickers';
import DayPicker from './DayPicker';
import WeekPicker from './WeekPicker';
import MonthPicker from './MonthPicker';
import YearPicker from './YearPicker';
import async from 'async';

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
			opacity: 0.5
		} 
	},
	tracker: { 
		line: { 
			stroke: red[500] 
		} 
	}
};

const styles = theme => ({
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
		alignItems: 'center',
		datetime: {
			width: theme.spacing(20)
		},
		power: {
			width: theme.spacing(20)
		}
	},
	chart: {
		marginTop: theme.spacing(1)
	}
});

class PowerChart extends Component {
	constructor(props) {
		super(props);
		// now, from & to must be 3 different objects
		const now = moment([moment().year(), moment().month(), moment().date(), 0, 0, 0]);
		const from = moment(now);
		const to = moment(from).add(1440, 'minutes');

		this.state = {
			ready: false,
			future: true,
			now: now,
			from: from,
			to: to,
			utcOffset: moment().utcOffset(),
			series: null,
			brushseries: null,
			timerange: null,
			brushrange: null,
			tracker: null,
			selection: null,
			aggregation: '1h',
			daterange: 'd',
			factor: 1000,
			load: 1,
			watch: 'r'
		};
	}

	getData = () => {
		const { record } = this.props;
		const { from, to, utcOffset, aggregation, factor, load, watch } = this.state;
		
		dataProvider(USAGE, 'meters', {
			data: { 
				cubeID: record.cubeID, 
				type: record.type,
				from,
				to,
				utcOffset,
				aggregation,
				factor,
				load,
				watch
			}
		})
		.then(response => response.data.usage)
		.then(usage => {
			const points = [];
			const brushPoints = [];

			if (usage.length === 0) {
				this.setState({ 
					ready: false 
				});
			} else {
				for (let i = 0; i < usage.length; i += 1) {
					const time = moment(usage[i].t);
					const value = usage[i].v;
					const index = Index.getIndexString(aggregation, time); 

					points.push([index, value]);					
					brushPoints.push([time, value]);
				}

				const series = new TimeSeries({
					name: "data",
					columns: ["index", "value"],
					points: points
				});

				const brushSeries = new TimeSeries({
					name: "brush",
					columns: ["time", "brush"],
					points: brushPoints
				});

				const initialRange = series && series.range();
				const minTime = initialRange && initialRange.begin();
				const maxTime = initialRange && initialRange.end();
				const minDuration = 3600000;
				
				this.setState({
					ready: true,
					series: series, 
					brushseries: brushSeries, 
					timerange: initialRange, 
					brushrange: initialRange,
					minTime,
					maxTime,
					minDuration
				});
			}
		});
	}
		
	componentDidMount() {
		const { record } = this.props;

		async.series([
			(callback) => {
				dataProvider(GET_ONE, `setup`, {
					id: record.cubeID
				})
				.then(response => response.data)
				.then(data => {
					callback(null, data)
				});
			}
		],
		(err, results) => {
			const data = results[0];
			const input = data && data.input;

			const factor = input && input.signal === 'p' && input.factor;
			const watch = input && input.signal === 'c' && input.watch;
			const load = input && input.signal === 'c' && input.load;

			if (record.type === 'p' && factor) {
				this.setState({
					factor
				});
			}

			if (record.type === 'c' && watch && load) {
				this.setState({
					watch,
					load
				});
			}

			this.getData();
		});
	}

	handleTrackerChanged = tracker => {
		this.setState({ tracker });
	}

	handleTimeRangeChange = timerange => {
		if (timerange) {
			this.setState({ timerange, brushrange: timerange });
		} else {
			const brushseries = this.state.brushseries;
			this.setState({ timerange: brushseries.range(), brushrange: null });
		}
	}

	handleAggregation = aggregation => {
		this.setState({ 
			aggregation
		},() => this.getData());
	}

	handleDateRange = daterange => {
		const { now } = this.state;
		let new_from, to, aggregation;

		switch(daterange) {
			case 'w':
				new_from = moment().startOf('week');
				to = moment(new_from).add(1, 'weeks');
				aggregation = '1h';
				break;
			case 'm':
				new_from = moment().startOf('month');
				to = moment(new_from).add(1, 'months');
				aggregation = '1h';
				break;
			case 'y':
				new_from = moment().startOf('year');
				to = moment(new_from).add(1, 'years');
				aggregation = '1h';
				break;
			case 'd':
			default:
				new_from = moment().startOf('day');
				to = moment(new_from).add(1, 'days');
				aggregation = '1h';
				break;
		}

		this.setState({ 
			future: now.diff(new_from, 'days') === 0,
			from: new_from,
			to,
			daterange,
			aggregation
		},() => this.getData());
	}
		
	handleDateChange = from => {
		const { now, daterange } = this.state;
		let new_from, to;
		switch(daterange) {
			case 'w':
				new_from = moment(from).startOf('week');
				to = moment(new_from).add(1, 'weeks');
				break;
			case 'm':
				new_from = moment(from).startOf('month');
				to = moment(new_from).add(1, 'months');
				break;
			case 'y':
				new_from = moment(from).startOf('year');
				to = moment(new_from).add(1, 'years');
				break;
			case 'd':
			default:
				new_from = moment(from).startOf('day');
				to = moment(new_from).add(1, 'days');
				break;
		}
		
		this.setState({ 
			future: now.diff(new_from, 'days') === 0,
			from: new_from,
			to 
		},() => this.getData());
	}
	
	handleStepBack = from => {
		const { now, daterange } = this.state;
		let new_from, to;
		switch(daterange) {
			case 'w':
				new_from = moment(from).subtract(1, 'weeks');
				to = moment(new_from).add(1, 'weeks');
				break;
			case 'm':
				new_from = moment(from).subtract(1, 'months');
				to = moment(new_from).add(1, 'months');
				break;
			case 'y':
				new_from = moment(from).subtract(1, 'years');
				to = moment(new_from).add(1, 'years');
				break;
			case 'd':
			default:
				new_from = moment(from).subtract(1, 'days');
				to = moment(new_from).add(1, 'days');
				break;
		}

		this.setState({ 
			future: now.diff(new_from, 'days') === 0,
			from: new_from,
			to 
		},() => this.getData());
	}

	handleStepForward = from => {
		const { now, daterange } = this.state;
		let new_from, to;
		switch(daterange) {
			case 'w':
				new_from = moment(from).add(1, 'weeks');
				to = moment(new_from).add(1, 'weeks');
				break;
			case 'm':
				new_from = moment(from).add(1, 'months');
				to = moment(new_from).add(1, 'months');
				break;
			case 'y':
				new_from = moment(from).add(1, 'years');
				to = moment(new_from).add(1, 'years');
				break;
			case 'd':
			default:
				new_from = moment(from).add(1, 'days');
				to = moment(new_from).add(1, 'days');
				break;
		}

		this.setState({ 
			future: now.diff(new_from, 'days') === 0,
			from: new_from,
			to 
		},() => this.getData());
	}

	render() {
		const { classes } = this.props;
		const { 
			ready,
			from,
			future,
			series,
			brushseries,
			brushrange,
			timerange,
			selection,
			highlight, 
			tracker,
			maxTime,
			minTime,
			minDuration,
			daterange,
			aggregation
		} = this.state;

		let selectedDate = '--';
		let selectedValue = '--';
		let renderCharts = <div className={classes.chart}>Loading.....</div>;

		if (ready && series && timerange) {
			selectedDate = selection &&
				`${moment(selection.event.index().begin()).format('MM/DD/YYYY, HH:mm')}`;

			selectedValue = selection &&
				`${selection.event.value(selection.column)} kW`;

			let infoValues = [];

			if (highlight) {
				const energyText = `${highlight.event.get(highlight.column)} kW`;
				infoValues = [{ label: 'Power', value: energyText }];
			}

			const seriesCropped = series.crop(timerange);

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
						onTimeRangeChanged={this.handleTimeRangeChange}
						onTrackerChanged={this.handleTrackerChanged}
					>
						<ChartRow height='400'>
							<YAxis
								id='axis'
								label='Power (kW)'
								min={0}
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
									infoTimeFormat={index => moment(index.begin()).format('MM/DD/YYYY, HH:mm')}
									infoStyle={chartstyle.infoBox}
									stemStyle={chartstyle.infoBox.stemStyle}
									markerStyle={chartstyle.infoBox.markerStyle}
									highlighted={highlight}
									onHighlightChange={h => this.setState({ highlight: h })}
									selected={selection}
									onSelectionChange={s => this.setState({ selection: s })}
								/>
							</Charts>
						</ChartRow>
					</ChartContainer>
				</Resizable>
				<Resizable>
					<ChartContainer
						format='%H:%M'
						utc={true}
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
								onTimeRangeChanged={this.handleTimeRangeChange}
							/>
							<YAxis
								id='axis1'
								min={0}
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
				renderCalendar = <WeekPicker date={from} onChange={this.handleDateChange} />
				break;
			case 'm':
				renderCalendar = <MonthPicker date={from} onChange={this.handleDateChange} />
				break;
			case 'y':
				renderCalendar = <YearPicker date={from} onChange={this.handleDateChange} />
				break;
			case 'd':
			default:
				renderCalendar = <DayPicker date={from} onChange={this.handleDateChange} />
				break;
		}
		
		return (
			<div>
				<Toolbar className={classes.toolbar} >
					<Typography variant="body2" component='span'>
						Date range: <SelectDateRange daterange={daterange} onChange={this.handleDateRange} />
					</Typography>
					<Typography variant="body2" component='span'>
						Aggregate by: <SelectAggregation aggregation={aggregation} daterange={daterange} onChange={this.handleAggregation} />
					</Typography>
					<div className={classes.calendar}>
						<Button className={classes.prevbtn}
							variant='outlined'
							label={null}
							disabled={false}
							onClick={() => this.handleStepBack(from)}
						>
							<PrevIcon />
						</Button>
						<MuiPickersUtilsProvider utils={MomentUtils}>
							{renderCalendar}
						</MuiPickersUtilsProvider>
						<Button className={classes.nextbtn}
							variant='outlined'
							label={null}
							alignIcon='right'
							disabled={future}
							onClick={() => this.handleStepForward(from)}
						>
							<NextIcon />
						</Button>
					</div>
				</Toolbar>

				<DividerFiled />

				{ renderCharts }

				<DividerFiled />

				<div className={classes.info}>
					<Typography variant="body2" style={{ color: grey[500] }}>
						Date/Time:&nbsp;
					</Typography>
					<Typography variant="body2" className={classes.info.datetime} >
						{selectedDate}&nbsp;
					</Typography>
					<Typography variant="body2" style={{ color: grey[500] }}>
						Power:&nbsp;
					</Typography>
					<Typography variant="body2" className={classes.info.power} style={{ color: red[500] }}>
						{selectedValue}
					</Typography>
				</div>

			</div>
		);
	}
}

PowerChart.propTypes = {
	classes: PropTypes.object.isRequired,
	record: PropTypes.object
};

export default withStyles(styles)(PowerChart);
