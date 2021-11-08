import PropTypes from 'prop-types';
import { useRecordContext, Labeled } from 'react-admin';
import Typography from '@material-ui/core/Typography';
import { green, red } from '@material-ui/core/colors';

const DutyCycleField = (props: any) => {
    const { className, label, source } = props;
    const record = useRecordContext(props);

    const value = parseInt(record[source], 10);
    const duty = value / 100;
    const cycle = parseInt(record['completeCycle'], 10);
    const on = Math.round(cycle * duty);
    const off = Math.round(cycle - on);

    return (
        <Labeled label={label}>
            <Typography className={className}
                component='span'
                variant='body2'                
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

DutyCycleField.defaultProps = {
    addLabel: true,
};

export default DutyCycleField;
