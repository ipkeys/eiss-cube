import moment from 'moment';
import { useRecordContext } from 'react-admin';
import Typography from '@mui/material/Typography';
import { DateTimeMomentFormat } from '../../../App';

const StartedAndLastPingField = (props: any) => {
	const record = useRecordContext(props);

	let started = '---',
		lastping = '---';

	if (record && record.timeStarted) {
		started = moment(record.timeStarted).format(DateTimeMomentFormat);
	}
	if (record && record.lastPing) {
		lastping = moment(record.lastPing).format(DateTimeMomentFormat);
	}

	return (
		<Typography variant="body2">
			<i>Started:</i> <b>{ started }</b> <i>Last ping:</i> <b>{ lastping }</b>
		</Typography>
	);
};

export default StartedAndLastPingField;
