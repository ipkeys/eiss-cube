import React from 'react';
import PropTypes from 'prop-types';
import pure from 'recompose/pure';
import Divider from '@material-ui/core/Divider';

const DividerField = () => {
    return (
        <Divider />
    );
};

DividerField.propTypes = {
    addLabel: PropTypes.bool,
};

DividerField.displayName = 'DividerField';
const PureDividerField = pure(DividerField);

PureDividerField.defaultProps = {
    addLabel: false,
};

export default PureDividerField;
