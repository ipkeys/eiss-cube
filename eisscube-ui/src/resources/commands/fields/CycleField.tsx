import PropTypes from 'prop-types';
import { useRecordContext, Labeled } from 'react-admin';
import Typography from '@mui/material/Typography';

const CycleField = (props: any) => {
	const { className, label, suffix, source } = props;
	const record = useRecordContext(props);

	const value = record[source];

	return (
		<Labeled label={label}>
			<Typography className={className}
				component='span'
				variant='body2'
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
	suffix: PropTypes.string
};

CycleField.displayName = 'CycleField';

CycleField.defaultProps = {
	addLabel: true
};

export default CycleField;
