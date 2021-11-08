import { useGetList, Loading, Error, TextInput, maxLength } from 'react-admin';
import { makeStyles } from '@material-ui/core/styles';
import FormGroup from '@material-ui/core/FormGroup';

const useStyles = makeStyles(theme => ({ 
    longText: {
        minWidth: theme.spacing(48) 
    },
    formGroup: {
        marginTop: theme.spacing(1)
    }
}));

const SettingsInput = (props: any) => {
    const classes = useStyles();
    const { data, ids, loading, error } = useGetList(
        'properties', 
        { page: 1, perPage: 100 },
        { field: 'name', order: 'ASC' },
        { type: props.type}
    );

    if (loading) return <Loading />;
    if (error) return <Error error={error} />;

    return (
        <>
            {ids && ids.map(id => (
                <FormGroup key={data[id].id} row>
                    <TextInput
                        className={classes.longText} 
                        source={`settings.${data[id].name}`}
                        label={data[id].label} 
                        helperText={data[id].description}
                        validate={[maxLength(50)]}
                    />
                </FormGroup>
            ))}
        </>
    );
};

export default SettingsInput;
