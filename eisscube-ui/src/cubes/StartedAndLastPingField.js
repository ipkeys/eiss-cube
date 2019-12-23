import React from 'react';
import PropTypes from 'prop-types';
import moment from 'moment';
import Typography from '@material-ui/core/Typography';
import { DateTimeMomentFormat } from '../globalExports';

const StartedAndLastPingField = ({ record = {} }) => {
    let started = '---',
        lastping = '---';

    if (record.timeStarted) {
        started = moment(record.timeStarted).format(DateTimeMomentFormat);
    }
    if (record.lastPing) {
        lastping = moment(record.lastPing).format(DateTimeMomentFormat);
    }
    
    return (
        <Typography variant="body1">
            <i>Started:</i> { started }&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<i>Last ping:</i> { lastping }
        </Typography>
    );
};

StartedAndLastPingField.propTypes = {
    record: PropTypes.object
};

export default StartedAndLastPingField;
