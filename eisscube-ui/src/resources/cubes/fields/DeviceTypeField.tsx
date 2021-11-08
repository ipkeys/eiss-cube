import PropTypes from 'prop-types';
import { useRecordContext } from 'react-admin';
import Typography from '@material-ui/core/Typography';

const DeviceTypeField = (props: any) => {
    const { source } = props;
    const record = useRecordContext(props);

    return (
        <Typography variant="body2">
            <span>{record[source] === "e" ? "4G" : "LoRa"}</span>
        </Typography>
    );
};

DeviceTypeField.propTypes = {
    label: PropTypes.string,
    record: PropTypes.object,
    source: PropTypes.string.isRequired,
};

export default DeviceTypeField;
