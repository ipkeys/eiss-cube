import React, { Component } from 'react';
import PropTypes from 'prop-types';
import { withStyles } from '@material-ui/core/styles';
import moment from 'moment';

import { GET_ONE } from 'react-admin';
import { USAGE } from '../providers/dataProvider';
import { dataProvider } from '../providers';

import { TimeSeries, TimeRange, TimeRangeEvent } from "pondjs";
import { Charts, ChartContainer, ChartRow, EventChart, Resizable, Brush } from "react-timeseries-charts";

import { timeFormat } from "d3-time-format";

import { Toolbar } from '@material-ui/core';
import Typography from '@material-ui/core/Typography';
import { grey, teal } from '@material-ui/core/colors';

import DividerFiled from '../Layout/DividerField';
import SelectAggregation from './SelectAggregation';
import MomentUtils from '@date-io/moment';
import { KeyboardDatePicker, MuiPickersUtilsProvider } from '@material-ui/pickers';
import async from 'async';

const chartstyle = {
	axis: {
		values: {
			fill: 'rgba(0, 0, 0, 0.87)',
			"font-size": 12
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
	date: {
		marginTop: theme.spacing(1)
	},
	chart: {
		marginTop: theme.spacing(1)
	}
});

const df = timeFormat("%m/%d/%Y, %H:%M");

class CycleChart extends Component {
	constructor(props) {
		super(props);
		const day = moment.utc().add(moment().utcOffset(), 'm');

		this.state = {
			ready: false,
			series: null,
			tracker: null,
			timerange: null,
			brushrange: null,
			day: day,
			aggregation: '1h',
			factor: 1000,
			watch: 'r'
		};

		this.handleTrackerChanged = this.handleTrackerChanged.bind(this);
		this.handleTimeRangeChange = this.handleTimeRangeChange.bind(this);
		this.handleAggregation = this.handleAggregation.bind(this);
	}

	getData = () => {
		const { record } = this.props;
		const { day, aggregation, factor, watch } = this.state;
		
		dataProvider(USAGE, 'meters', {
			data: { 
				cubeID: record.cubeID, 
				type: record.type,
				day,
				aggregation,
				factor,
				watch
			}
		})
		.then(response => response.data.usage)
		.then(usage => {

			const events = [];
			let lastReportTime = moment();

			for (let i = 0; i < usage.length; i += 1) {
				const startTime = new Date(usage.timestamp);
				const endTime = new Date(startTime.getTime() + usage.value * 1000); // add duration
				
				events.push(new TimeRangeEvent(new TimeRange(startTime, endTime), { title: `ON (${usage.value}sec)` }));
				/*
				if (i > 0) {
					// insert "null" to break the line chart
					const deltaTime = time - new Date(usage[i - 1].timestamp);
					switch(aggregation) {
						case "1m":
							if (deltaTime > 60000) {
								brushPoints.push([time, null]);
							}
							break;
						case "5m":
							if (deltaTime > 5*60000) {
								brushPoints.push([time, null]);
							}
							break;
						case "15m":
							if (deltaTime > 15*60000) {
								brushPoints.push([time, null]);
							}
							break;
						case "30m":
							if (deltaTime > 30*60000) {
								brushPoints.push([time, null]);
							}
							break;
						case "1h":
						default:
							if (deltaTime > 60*60000) {
								brushPoints.push([time, null]);
							}
							break;
					}
				}
				brushPoints.push([time, usage[i].value]);

				if (i === usage.length-1) {
					lastReportTime = time;
				}
				*/
			}
		
			const series = new TimeSeries({ 
				name: "cycle", 
				events: events
			});

			let timebefore = moment(lastReportTime).subtract(24, 'hour');
			let timeafter = moment(lastReportTime).add(1, 'hour');

			const initialRange = new TimeRange([timebefore, timeafter]);
			const minTime = series && series.range() && series.range().begin();
			const maxTime = series && series.range() && series.range().end();
			const minDuration = 3600000;

			this.setState({ 
				ready: true,
				series: series, 
				timerange: initialRange,
				brushrange: initialRange,
				minTime,
				maxTime,
				minDuration
			});		

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

			if (record.type === 'p' && factor) {
				this.setState({
					factor
				});
			}

			if (record.type === 'c' && watch) {
				this.setState({
					watch
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
			const series = this.state.series;
			this.setState({ timerange: series.range(), brushrange: null });
		}
	}

	eventStyleFunc(event, state) {
		switch (state) {
			case "normal":
				return {
					fill: teal[200]
				};
			case "hover":
				return {
					fill: teal[50]
				};
			case "selected":
				return {
					fill: teal[200]
				};
			default:
			//pass
		}
	}

	handleAggregation(aggregation) {
		this.setState({ aggregation });
	}

	handleDateChange(d) {
		const day = moment.utc([d.year(), d.month(), d.date(), 0, 0, 0]).subtract(moment().utcOffset(), 'm');
		this.setState({ day });
	}

	render() {
		const { classes } = this.props;
		const {
			ready, 
			series,
			brushrange,
			tracker,
			timerange,
			maxTime,
			minTime,
			minDuration
		} = this.state;

		let renderCharts = <div className={classes.chart}>Loading...</div>;
		if (!ready) {
			renderCharts = <div className={classes.chart}>Not available yet...</div>;
		} else if (series && timerange) {
			renderCharts =
				<div className={classes.chart}>
					<Resizable>
						<ChartContainer
							format='%H:%M'
							timeAxisStyle={chartstyle.axis}
							timeRange={timerange}
							maxTime={maxTime}
							minTime={minTime}
							minDuration={minDuration}
							trackerPosition={tracker}
							onTrackerChanged={this.handleTrackerChanged}
							enablePanZoom={true}
							onTimeRangeChanged={this.handleTimeRangeChange}
							minDuration={60 * 60 * 24 * 30}
						>
							<ChartRow height="80">
								<Brush
									timeRange={brushrange}
									allowSelectionClear
									onTimeRangeChanged={this.handleTimeRangeChange}
								/>
								<Charts>
									<EventChart
										series={series}
										size={60}
										style={this.eventStyleFunc}
										label={e => e.get("title")}
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
					<MuiPickersUtilsProvider utils={MomentUtils}>
						<KeyboardDatePicker
							className={classes.date}
							autoOk
							variant='inline'
							label={false}
							format='MM/DD/YYYY'
							value={this.state.day}
							disableFuture={true}
							onChange={date => this.handleDateChange(date)}
						/>
					</MuiPickersUtilsProvider>
				</Toolbar>

				<DividerFiled />

				{ renderCharts }

				<DividerFiled />

				<div className={classes.info}>
					<Typography variant="body2" style={{ color: grey[500] }}>
						Date/Time: {tracker ? `${df(tracker)}` : ""}
					</Typography>
				</div>
			</div>
		);
	}
}

CycleChart.propTypes = {
	classes: PropTypes.object.isRequired,
	record: PropTypes.object
};

export default withStyles(styles)(CycleChart);
