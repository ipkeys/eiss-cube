import { makeStyles } from '@material-ui/core/styles';

const useStyles = makeStyles(theme => ({
    logo: {
        background: 'url(Logo.png)',
        backgroundRepeat: 'no-repeat',
        minHeight: theme.spacing(10),
        marginTop: theme.spacing(3),
        marginLeft: theme.spacing(3),
        marginRight: theme.spacing(2)
    }
}));

const Logo = () => {
    const classes = useStyles();
    return (
        <div className={classes.logo} />
    );
}

export default Logo;
