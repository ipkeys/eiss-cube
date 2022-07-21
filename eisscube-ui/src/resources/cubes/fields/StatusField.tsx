import moment from 'moment';
import { useRecordContext } from 'react-admin';
import Typography from '@mui/material/Typography';
import { green, red } from '@mui/material/colors';

const StatusField = (props: any) => {
    const record = useRecordContext(props);

	if (!record) return null;

	let dur = '';

	if (record.online === true) {
		if (record.timeStarted !== 'undefined') {
			dur = moment.duration(moment().valueOf() - moment(new Date(record.timeStarted)).valueOf()).humanize();
		}
		return (
			<Typography variant="body2">
				<span style={{ color: green[500] }}><b>ONLINE</b></span> { dur !== 'Invalid date' ? dur : '' }
			</Typography>
		);
	} else {
		if (record.lastPing !== 'undefined') {
			dur = moment.duration(moment().valueOf() - moment(new Date(record.lastPing)).valueOf()).humanize();
		}
		return (
			<Typography variant="body2">
				<span style={{ color: red[500] }}><b>OFFLINE</b></span> { dur !== 'Invalid date' ? dur : '' }
			</Typography>
		);
	}
};

export default StatusField;
