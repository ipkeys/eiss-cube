import React from 'react';
import PropTypes from 'prop-types';
import pure from 'recompose/pure';
import get from 'lodash.get';
import FormControlLabel from '@material-ui/core/FormControlLabel';
import Checkbox from '@material-ui/core/Checkbox';

import sanitizeRestProps from './sanitizeRestProps';

const TargetField = ({ className, label, source, record = {}, suffix, ...rest }) => {
    const value = get(record, source);
    return (
        <FormControlLabel
          control={
            <Checkbox
              checked={ value }
              color="primary"
              className={className}
              {...sanitizeRestProps(rest)}
          />
          }
          label={label}
        />
    );
};

TargetField.propTypes = {
    addLabel: PropTypes.bool,
    basePath: PropTypes.string,
    className: PropTypes.string,
    cellClassName: PropTypes.string,
    headerClassName: PropTypes.string,
    label: PropTypes.string,
    record: PropTypes.object,
    sortBy: PropTypes.string,
    source: PropTypes.string.isRequired
};

TargetField.displayName = 'TargetField';
const PureTargetField = pure(TargetField);

PureTargetField.defaultProps = {
    addLabel: true,
};

export default PureTargetField;
