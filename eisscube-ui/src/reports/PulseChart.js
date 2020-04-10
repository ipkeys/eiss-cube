import React, { Component, Fragment } from 'react';
import PropTypes from 'prop-types';
import { withStyles } from '@material-ui/core/styles';
import moment from 'moment';

import { GET_ONE, Button } from 'react-admin';
import { USAGE } from '../providers/dataProvider';
import { dataProvider } from '../providers';

import { TimeSeries, TimeRange, Index } from "pondjs";
import { Charts, ChartContainer, ChartRow, YAxis, AreaChart, BarChart, Resizable, Brush } from "react-timeseries-charts";

import { Toolbar } from '@material-ui/core';
import Typography from '@material-ui/core/Typography';
import PrevIcon from '@material-ui/icons/ArrowBackIos';
import NextIcon from '@material-ui/icons/ArrowForwardIos';

import { grey, red, teal } from '@material-ui/core/colors';
import DividerFiled from '../Layout/DividerField';
import SelectAggregation from './SelectAggregation';
import MomentUtils from '@date-io/moment';
import { DatePicker, MuiPickersUtilsProvider } from '@material-ui/pickers';
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
		textStyle: {
			fill: 'rgba(0, 0, 0, 0.87)',
			'font-size': 12
		},
		line: { 
			stroke: red[500], 
			cursor: 'crosshair', 
			pointerEvents: 'none' 
		}, 
		box: { 
			fill: red[50], 
			opacity: 0.5, 
			stroke: red[500], 
			pointerEvents: 'none' 
		}, 
		dot: { 
			fill: "#000" 
		} 
	},
	tracker: { 
		line: { 
			stroke: red[500], 
			cursor: 'crosshair', 
			pointerEvents: 'none' 
		}, 
		box: { 
			fill: red[50], 
			opacity: 0.5, 
			stroke: red[500], 
			pointerEvents: 'none' 
		}, 
		dot: { 
			fill: red[500] 
		} 
	}
};

const styles = theme => ({
	toolbar: {
		paddingLeft: 0,
		paddingRight: 0,
		flex: 1,
		display: 'flex',
		justifyContent: 'space-between'
	},
	calendar: {
		alignContent: 'center',
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

class PulseChart extends Component {
	constructor(props) {
		super(props);
		const d = moment(); 
		const day = moment([d.year(), d.month(), d.date(), 0, 0, 0]); // remove h:m:s

		this.state = {
			ready: false,
			now: day.date(),
			day: day,
			utcOffset: moment().utcOffset(),
			series: null,
			brushseries: null,
			timerange: null,
			brushrange: null,
			tracker: null,
			selection: null,
			aggregation: '1h',
			factor: 1000,
			load: 1,
			watch: 'r'
		};

		this.handleTrackerChanged = this.handleTrackerChanged.bind(this);
		this.handleTimeRangeChange = this.handleTimeRangeChange.bind(this);
		this.handleAggregation = this.handleAggregation.bind(this);
		this.handleDateChange = this.handleDateChange.bind(this);
		this.handleNextDay = this.handleNextDay.bind(this);
		this.handlePrevDay = this.handlePrevDay.bind(this);
	}

	getData = () => {
		const { record } = this.props;
		const { day, utcOffset, aggregation, factor, load, watch } = this.state;
		
		dataProvider(USAGE, 'meters', {
			data: { 
				cubeID: record.cubeID, 
				type: record.type,
				day,
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
			let lastReportTime = moment();

			if (usage.length === 0) {
				this.setState({ 
					ready: false 
				});
			} else {
				for (let i = 0; i < usage.length; i += 1) {
					const time = moment(usage[i].timestamp); //new Date(usage[i].timestamp);

					points.push([Index.getIndexString(aggregation, time), usage[i].value]);
					
					if (i > 0) {
						// insert "null" to break the line chart
						const deltaTime = moment.duration(time.diff(moment(usage[i-1].timestamp))).asMinutes(); //   time -  new Date(usage[i - 1].timestamp);
						switch(aggregation) {
							case "1m":
								if (deltaTime > 1) {
									brushPoints.push([time, null]);
								}
								break;
							case "5m":
								if (deltaTime > 5) {
									brushPoints.push([time, null]);
								}
								break;
							case "15m":
								if (deltaTime > 15) {
									brushPoints.push([time, null]);
								}
								break;
							case "30m":
								if (deltaTime > 30) {
									brushPoints.push([time, null]);
								}
								break;
							case "1h":
							default:
								if (deltaTime > 60) {
									brushPoints.push([time, null]);
								}
								break;
						}
					}
					brushPoints.push([time, usage[i].value]);

					if (i === usage.length-1) {
						lastReportTime = time;
					}
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

				let timebefore = moment(lastReportTime).subtract(1439, 'minute');
				let timeafter = moment(lastReportTime).add(59, 'minute');
				switch(aggregation) {
					case "1m":
						timebefore = moment(lastReportTime).subtract(24, 'minute');
						timeafter = moment(lastReportTime).add(1, 'minute');
						break;
					case "5m":
						timebefore = moment(lastReportTime).subtract(2, 'hour');
						timeafter = moment(lastReportTime).add(5, 'minute');
						break;
					case "15m":
						timebefore = moment(lastReportTime).subtract(6, 'hour');
						timeafter = moment(lastReportTime).add(15, 'minute');
						break;
					case "30m":
						timebefore = moment(lastReportTime).subtract(12, 'hour');
						timeafter = moment(lastReportTime).add(30, 'minute');
						break;
					case "1h":
					default:
						break;
				}
				
				const initialRange = new TimeRange([timebefore, timeafter]);
				const minTime = series && series.range() && series.range().begin();
				const maxTime = series && series.range() && series.range().end();
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

	componentDidUpdate(prevProps, prevState, snapshot) {
		const { aggregation, day } = this.state;
		
		if (aggregation !== prevState.aggregation || moment(day).date() !== moment(prevState.day).date()) {
			this.getData();
		}
	}

	handleTrackerChanged(tracker) {
		this.setState({ tracker });
	}

	handleTimeRangeChange(timerange) {
		if (timerange) {
			this.setState({ timerange, brushrange: timerange });
		} else {
			const brushseries = this.state.brushseries;
			this.setState({ timerange: brushseries.range(), brushrange: null });
		}
	}

	handleAggregation(aggregation) {
		this.setState({ aggregation });
	}
		
	handleDateChange(d) {
		const day = moment([d.year(), d.month(), d.date(), 0, 0, 0]); // remove h:m:s
		this.setState({ day });
	}
	
	handlePrevDay(day) {
		this.setState({ 
			day: day.subtract(1, 'd') 
		});

		this.getData();
	}
	
	handleNextDay(day) {
		this.setState({ 
			day: day.add(1, 'd')
		});

		this.getData();
	}

	render() {
		const { classes } = this.props;
		const { 
			ready,
			day,
			now,
			series,
			brushseries,
			brushrange,
			timerange,
			selection,
			highlight, 
			tracker,
			maxTime,
			minTime,
			minDuration
		} = this.state;

		let selectedDate = '--';
		let selectedValue = '--';
		let renderCharts = <div className={classes.chart}>Loading.....</div>;

		if (!ready) {
			renderCharts = <div className={classes.chart}>Data not available...</div>;
		} else if (series && timerange) {
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
						<ChartRow height="400">
							<YAxis
								id="axis"
								label="Power (kW)"
								min={0}
								max={series.max() + series.max() * 0.1}
								width={60}
								valWidth={40}
								format = ",.2f"
								type="linear"
								style={chartstyle.axis}
							/>
							<Charts>
								<BarChart
									axis="axis"
									style={chartstyle}
									columns={["value"]}
									series={seriesCropped}
									breakLine
									info={infoValues}
									infoWidth={100}
									infoOffsetY={5}
									infoTimeFormat={index => moment(index.begin()).format("MM/DD/YYYY, HH:mm")}
									infoStyle={chartstyle.infoBox}
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
						<ChartRow height="80">
							<Brush
								timeRange={brushrange}
								allowSelectionClear
								onTimeRangeChanged={this.handleTimeRangeChange}
							/>
							<YAxis
								id="axis1"
								min={0}
								max={brushseries.max("brush") + brushseries.max("brush") * 0.1}
								width={60}
								valWidth={40}
								format = ",.2f"
								style={chartstyle.axis}		
							/>
							<Charts>
								<AreaChart
									axis="axis1"
									style={chartstyle}
									columns={{ up: ["brush"], down: [] }}
									series={brushseries}
								/>
							</Charts>
						</ChartRow>
					</ChartContainer>
				</Resizable>
			</div>;
		}

		return (
			<div>
				<Toolbar className={classes.toolbar} >
					<Typography variant="body2" component='span'>
						Aggregate by: <SelectAggregation onChange={this.handleAggregation} />
					</Typography>
					<div className={classes.calendar}>
						<Button className={classes.prevbtn}
							variant='outlined'
							label={null}
							disabled={false}
							onClick={() => this.handlePrevDay(day)}
						>
							<PrevIcon />
						</Button>
						<MuiPickersUtilsProvider utils={MomentUtils}>
							<DatePicker
								className={classes.date}
								autoOk
								variant='inline'
								label={false}
								format='MM/DD/YYYY'
								value={day}
								disableFuture={true}
								onChange={date => this.handleDateChange(date)}
							/>
						</MuiPickersUtilsProvider>
						<Button className={classes.nextbtn}
							variant='outlined'
							label={null}
							alignIcon='right'
							disabled={now === day.date()}
							onClick={() => this.handleNextDay(day)}
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

PulseChart.propTypes = {
	classes: PropTypes.object.isRequired,
	record: PropTypes.object
};

export default withStyles(styles)(PulseChart);
