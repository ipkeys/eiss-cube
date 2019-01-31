import React from 'react';
import PropTypes from 'prop-types';
import get from 'lodash/get';
import pure from 'recompose/pure';
import { Labeled } from 'react-admin';
import Typography from '@material-ui/core/Typography';
import { green, red } from '@material-ui/core/colors';

const DutyCycleField = ({ className, label, source, record = {} }) => {
    const value = parseInt(get(record, source), 10);
    const duty = value / 100;
    const cycle = parseInt(get(record, 'completeCycle'), 10);
    const on = Math.round(cycle * duty);
    const off = Math.round(cycle - on);
    return (
        <Labeled label={label}>
            <Typography
                component="span"
                body1="body1"
                className={className}
            >
                {value}% (<span style={{ color: green[500] }}>{on} sec ON</span> / <span style={{ color: red[500] }}>{off} sec OFF</span>)
            </Typography>
        </Labeled>
    );
};

DutyCycleField.propTypes = {
    addLabel: PropTypes.bool,
    label: PropTypes.string,
    source: PropTypes.string.isRequired,
    record: PropTypes.object,
    className: PropTypes.string,
  };

DutyCycleField.displayName = 'DutyCycleField';
const PureDutyCycleField = pure(DutyCycleField);

PureDutyCycleField.defaultProps = {
    addLabel: true,
};

export default PureDutyCycleField;
