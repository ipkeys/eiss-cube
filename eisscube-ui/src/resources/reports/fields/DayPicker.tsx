import { useState, useEffect } from "react";
import { TextField } from '@mui/material';
import { AdapterMoment } from '@mui/x-date-pickers/AdapterMoment';
import { LocalizationProvider } from '@mui/x-date-pickers/LocalizationProvider';
import { DatePicker } from "@mui/x-date-pickers/DatePicker";

const DayPicker = (props: any) => {
	const { date, onChange } = props;
	const [value, setValue] = useState(date);

	useEffect(() => {
		setValue(date);
	}, [date]);

	return (
		<LocalizationProvider dateAdapter={AdapterMoment}>
			<DatePicker disableFuture
				views={['day']}
				label="Day"
				value={value}
				onChange={(newValue) => {
					setValue(newValue);
					onChange(newValue);
				}}
				renderInput={(params) => <TextField sx={{width: 200}} {...params} />}
				componentsProps={{
					actionBar: {
						actions: ['today']
					},
				}}
			/>
		</LocalizationProvider>
	);
}

export default DayPicker;
