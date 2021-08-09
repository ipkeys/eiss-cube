import PropTypes from 'prop-types';

const DeviceTypeField = ({ record = {} }) => {
	return (record.deviceType === "e" ? "4G" : "LoRa");
};

DeviceTypeField.propTypes = {
    record: PropTypes.object
};

export default DeviceTypeField;
