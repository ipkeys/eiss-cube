import { makeStyles, Theme } from '@material-ui/core/styles';
import { 
    Edit,
    SimpleForm,
    SelectInput, 
    TextInput, 
    required,
    maxLength
} from 'react-admin';
import { NavTitle, PropertyTypes } from '../common';

const useStyles = makeStyles((theme: Theme) => ({ 
    longText: {
        minWidth: theme.spacing(66) 
    },
    inline: { 
        display: 'inline-block', 
        marginRight: theme.spacing(2)
    },
}));

const validateValue = [required(), maxLength(20)];

const PropertyEdit = (props: any) => {
    const classes = useStyles();

    return (
        <Edit  
            title={<NavTitle title='Edit Custom Property' {...props} />}
            {...props}
        >
            <SimpleForm redirect='list'>
                <SelectInput source='type' choices={PropertyTypes} />
                <TextInput label='Name' source='name' validate={validateValue} formClassName={classes.inline} />
                <TextInput label='Label' source='label' validate={validateValue} formClassName={classes.inline} />
                <TextInput label='Description' source='description' validate={[maxLength(50)]} className={classes.longText} />
            </SimpleForm>
        </Edit>
    );
};

export default PropertyEdit;
