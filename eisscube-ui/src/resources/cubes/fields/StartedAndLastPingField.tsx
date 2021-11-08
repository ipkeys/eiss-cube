import PropTypes from 'prop-types';
import moment from 'moment';
import { useRecordContext } from 'react-admin';
import Typography from '@material-ui/core/Typography';
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
            <i>Started:</i> { started } | <i>Last ping:</i> { lastping }
        </Typography>
    );
};

StartedAndLastPingField.propTypes = {
    label: PropTypes.string,
    record: PropTypes.object,
    source: PropTypes.string.isRequired,
};

export default StartedAndLastPingField;
