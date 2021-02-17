import React from 'react';
import PropTypes from 'prop-types';
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

DividerField.defaultProps = {
    addLabel: false,
};

export default DividerField;
