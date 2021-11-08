import { makeStyles, Theme } from '@material-ui/core/styles';
import ChevronLeft from '@material-ui/icons/ChevronLeft';
import { 
    Show, 
    SimpleShowLayout, 
    SelectField,
    TextField, 
    TopToolbar, 
    EditButton,
    ListButton,
    usePermissions
 } from 'react-admin';
import { NavTitle, DividerField, PropertyTypes } from '../common';
import { canEdit } from '../common/permissions';

const useStyles = makeStyles((theme: Theme) => ({ 
    longText: {
        minWidth: theme.spacing(66) 
    },
    inline: { 
        display: 'inline-block',
        minWidth: theme.spacing(24)   
    }
}));

const PropertyShowActions = ({data, basePath}: any) => {
    const { permissions } = usePermissions();

    return (
        <TopToolbar>
            <ListButton basePath={basePath} label="Back" icon={<ChevronLeft />} />
            { canEdit(data, permissions) && <EditButton basePath={basePath} record={data} /> }
        </TopToolbar>
    );
};

const PropertyShow = (props: any) => {
    const classes = useStyles();
    
    return (
        <Show
            title={<NavTitle title='View Custom Property' {...props} />}
            actions={<PropertyShowActions {...props} />}
            {...props}
        >
            <SimpleShowLayout>
                <SelectField source='type' choices={PropertyTypes} />
                <TextField label='Name' source='name' className={classes.inline} />
                <TextField label='Label' source='label' className={classes.inline} />

                <DividerField />

                <TextField label='Description' source='description' className={classes.longText} />
            </SimpleShowLayout>
        </Show>
    );
}

export default PropertyShow;
