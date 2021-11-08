import { makeStyles, Theme } from '@material-ui/core/styles';
import ChevronLeft from '@material-ui/icons/ChevronLeft';
import { 
    Create, 
    SelectInput, 
    SimpleForm, 
    TextInput,
    TopToolbar,
    ListButton,
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

const PropertyCreateActions = ({data, basePath}: any) => {
    return (
        <TopToolbar>
            <ListButton basePath={basePath} label="Back" icon={<ChevronLeft />} />
        </TopToolbar>
    );
};

const validateValue = [required(), maxLength(20)];

const PropertyCreate = (props: any) => {
    const classes = useStyles();

    return (
        <Create 
            title={<NavTitle title='Create a new Custom Property' {...props} />} 
            actions={<PropertyCreateActions {...props} />}
            {...props}
        >
            <SimpleForm redirect='list'>
                <SelectInput source='type' choices={PropertyTypes} />
                <TextInput label='Name' source='name' validate={validateValue} formClassName={classes.inline} />
                <TextInput label='Label' source='label' validate={validateValue} formClassName={classes.inline} />
                <TextInput label='Description' source='description' validate={[maxLength(50)]} className={classes.longText} />
            </SimpleForm>
        </Create>
    );
};

export default PropertyCreate;
