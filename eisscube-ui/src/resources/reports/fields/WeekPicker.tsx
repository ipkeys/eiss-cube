import * as React from 'react';
import { useState, useEffect } from "react";
import { styled } from '@mui/material/styles';
import { TextField } from '@mui/material';
import { AdapterMoment } from '@mui/x-date-pickers/AdapterMoment';
import { LocalizationProvider } from '@mui/x-date-pickers/LocalizationProvider';
import { DatePicker } from "@mui/x-date-pickers/DatePicker";
import { PickersDay, PickersDayProps } from '@mui/x-date-pickers/PickersDay';
import moment from "moment";

type PickerDayOfWeekProps = PickersDayProps<Date> & {
	dayIsBetween: boolean;
	isFirstDay: boolean;
	isLastDay: boolean;
};

const PickersDayOfWeek = styled(
		PickersDay,
		{ shouldForwardProp: (prop) => prop !== 'dayIsBetween' && prop !== 'isFirstDay' && prop !== 'isLastDay' }
	)<PickerDayOfWeekProps>(
		({ theme, dayIsBetween, isFirstDay, isLastDay }) => ({
		...(dayIsBetween && {
			borderRadius: '50%',
			backgroundColor: theme.palette.primary.main,
			color: theme.palette.common.white,
			'&:hover, &:focus': {
				backgroundColor: theme.palette.primary.dark
			}
		}),
		...(isFirstDay && {
			borderRadius: '50%'
		}),
		...(isLastDay && {
			borderRadius: '50%'
		})
	})
) as React.ComponentType<PickerDayOfWeekProps>;

const WeekPicker = (props: any) => {
	const { date, onChange } = props;
	const [value, setValue] = useState<Date | null>(date);

	useEffect(() => {
		setValue(date);
	}, [date]);

	const renderWeekPickerDay = (
		dt: Date,
		_selectedDates: Array<Date | null>,
		pickersDayProps: PickersDayProps<Date>
	) => {
		if (!value) {
			return <PickersDay {...pickersDayProps} />;
		}

		const start = moment(value).startOf('week');
		const end = moment(value).endOf('week');

		const dayIsBetween = moment(dt).isBetween(start, end);
		const isFirstDay = moment(dt).isSame(start);
		const isLastDay = moment(dt).isSame(end);

		return <PickersDayOfWeek
			{...pickersDayProps}
			showDaysOutsideCurrentMonth
			dayIsBetween={dayIsBetween}
			isFirstDay={isFirstDay}
			isLastDay={isLastDay}
		/>;
	};

	return (
		<LocalizationProvider dateAdapter={AdapterMoment}>
			<DatePicker disableFuture
				views={['day']}
				label='Week of'
				value={value}
				onChange={(newValue) => {
					setValue(newValue);
					onChange(newValue);
				}}
				renderDay={renderWeekPickerDay}
				renderInput={(params) => <TextField sx={{width: 200}} {...params} />}
				//inputFormat='MMM d'
			/>
		</LocalizationProvider>
	);
}

export default WeekPicker;
