import { useState, useEffect } from "react";
import { TextField } from '@mui/material';
import { AdapterMoment } from '@mui/x-date-pickers/AdapterMoment';
import { LocalizationProvider } from '@mui/x-date-pickers/LocalizationProvider';
import { DatePicker } from "@mui/x-date-pickers/DatePicker";

const YearPicker = (props: any) => {
	const { date, onChange } = props;
	const [value, setValue] = useState(date);

	useEffect(() => {
		setValue(date);
	}, [date]);

	return (
		<LocalizationProvider dateAdapter={AdapterMoment}>
			<DatePicker disableFuture
				views={['year']}
				label='Year'
				value={value}
				onChange={(newValue) => {
					setValue(newValue);
					onChange(newValue);
				}}
				renderInput={(params) => <TextField sx={{width: 200}} {...params} />}
			/>
		</LocalizationProvider>
	);
}

export default YearPicker;
