import { makeStyles, Theme } from '@material-ui/core/styles';
import { 
    TextInput,
    ReferenceInput,
    AutocompleteInput,
    Edit,
    TabbedForm,
    FormTab,
    maxLength,
    usePermissions
} from 'react-admin';

import SettingsInput from '../common/SettingsInput';
import { isSuper } from '../common/Roles';
import NavTitle from '../common/NavTitle';

const useStyles = makeStyles((theme: Theme) => ({ 
    inlineField: { 
        display: 'inline-block',
        marginRight: theme.spacing(2), 
        minWidth: theme.spacing(24)   
    },
    inline: { 
        display: 'inline-block', 
        marginRight: theme.spacing(2)
    },
    longText: {
        minWidth: theme.spacing(66) 
    }
}));

const validateName = maxLength(50);

const CubeEdit = (props: any ) => {
    const classes = useStyles();
    const { permissions } = usePermissions();

    return (
        <Edit
            title={<NavTitle title='Edit EISS™Cube' />}
            {...props}
        >
            <TabbedForm>
                <FormTab label='identity'>
                <TextInput disabled label='ID' source='deviceID' className={classes.longText} />
                <TextInput label='Name' source='name' formClassName={classes.inline} validate={validateName} />
                    {isSuper(permissions) &&
                        <ReferenceInput 
                            sort={{ field: 'displayName', order: 'ASC' }}
                            source="group_id" 
                            reference="grps"
                            formClassName={classes.inline}
                        >
                            <AutocompleteInput optionText='displayName' formClassName={classes.inline} />
                        </ReferenceInput>
                    }
                </FormTab>
                <FormTab label='customer'>
                    <TextInput label='Customer ID' source='customerID' className={classes.longText} />
                    <TextInput label='Zone' source='zone' formClassName={classes.inline} />
                    <TextInput label='Subzone' source='subZone' formClassName={classes.inline} />
               </FormTab>
                <FormTab label='address'>
                    <TextInput label='Address' source='address' className={classes.longText} />
                    <TextInput label='City, State' source='city' formClassName={classes.inline} />
                    <TextInput label='Zip Code' source='zipCode' formClassName={classes.inline} />
                </FormTab>
                <FormTab label='settings'>
                    <SettingsInput />
               </FormTab>
            </TabbedForm>
        </Edit>
    );
};

export default CubeEdit;
