import PropTypes from 'prop-types';
import { makeStyles, Theme } from '@material-ui/core/styles';
import Divider from '@material-ui/core/Divider';

const useStyles = makeStyles((theme: Theme) => ({
    divider: {
        marginTop: theme.spacing(1)
	}
}));

const DividerField = () => {
    const classes = useStyles();
    return (
        <Divider className={classes.divider} />
    );
}

DividerField.propTypes = {
    addLabel: PropTypes.bool,
};

DividerField.displayName = 'DividerField';

DividerField.defaultProps = {
    addLabel: false,
};

export default DividerField;
