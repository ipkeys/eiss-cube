import React from 'react';
import PropTypes from 'prop-types';
import get from 'lodash/get';
import pure from 'recompose/pure';
import { Labeled } from 'react-admin';
import Typography from '@material-ui/core/Typography';

import sanitizeRestProps from './sanitizeRestProps';

const CycleField = ({ className, label, source, record = {}, suffix, ...rest }) => {
    const value = get(record, source);
    return (
        <Labeled label={label}>
            <Typography
                component="span"
                body1="body1"
                className={className}
                {...sanitizeRestProps(rest)}
            >
                {value && typeof value !== 'string' ? JSON.stringify(value) : value} {suffix}
            </Typography>
        </Labeled>
    );
};

CycleField.propTypes = {
    addLabel: PropTypes.bool,
    basePath: PropTypes.string,
    className: PropTypes.string,
    cellClassName: PropTypes.string,
    headerClassName: PropTypes.string,
    label: PropTypes.string,
    record: PropTypes.object,
    sortBy: PropTypes.string,
    source: PropTypes.string.isRequired,
    suffix: PropTypes.string
};

CycleField.displayName = 'CycleField';
const PureCycleField = pure(CycleField);

PureCycleField.defaultProps = {
    addLabel: true,
};

export default PureCycleField;
