import React from 'react';
import PropTypes from 'prop-types';
import get from 'lodash/get';
import pure from 'recompose/pure';
import { Labeled } from 'react-admin';
import Typography from '@material-ui/core/Typography';

const CycleField = ({ className, label, source, record = {}, suffix }) => {
    const value = get(record, source);
    return (
        <Labeled label={label}>
            <Typography
                component="span"
                body1="body1"
                className={className}
            >
                {value && typeof value !== 'string' ? JSON.stringify(value) : value} {suffix}
            </Typography>
        </Labeled>
    );
};

CycleField.propTypes = {
    addLabel: PropTypes.bool,
    label: PropTypes.string,
    source: PropTypes.string.isRequired,
    record: PropTypes.object,
    suffix: PropTypes.string,
    className: PropTypes.string
};

CycleField.displayName = 'CycleField';
const PureCycleField = pure(CycleField);

PureCycleField.defaultProps = {
    addLabel: true,
};

export default PureCycleField;
