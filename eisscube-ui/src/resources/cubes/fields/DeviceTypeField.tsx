import { useRecordContext } from 'react-admin';
import Typography from '@mui/material/Typography';

const DeviceTypeField = (props: any) => {
	const { source } = props;
	const record = useRecordContext(props);

	return (
		<Typography variant="body2">
			<span>{record[source] === "e" ? "4G" : "LoRa"}</span>
		</Typography>
	);
};

export default DeviceTypeField;
