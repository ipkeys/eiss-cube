import React from 'react';
import { 
    downloadCSV,
    List,
    Edit,
    Create,
    Filter,
    Datagrid,
    TextField,
    SearchInput,
    TextInput,
    EditButton,
    Responsive, 
    SimpleList,
    SimpleForm,
    required,
    maxLength
} from 'react-admin';
import { unparse as convertToCSV } from 'papaparse/papaparse.min';
import Icon from '@material-ui/icons/LocalOffer';
import { withStyles } from '@material-ui/core/styles';

export const PropertyIcon = Icon;

const styles = theme => ({
    title: {
        color: theme.palette.common.white
    },
    rowEven: {
        backgroundColor: theme.palette.grey[100]
    },
    showLayout: {
        paddingTop: theme.spacing.unit * 2
    },
    inline: { 
        display: 'inline-block', 
        marginRight: theme.spacing.unit * 2
    },
    longText: {
        minWidth: theme.spacing.unit * 66 
    }
});

const exporter = records => {
    const data = records.map(record => ({
        ...record
    }));

    const csv = convertToCSV({
        data,
        fields: ['name', 'label', 'description']
    });

    downloadCSV(csv, 'EISS™Cubes Properties');
};

const PropertyTitle = withStyles(styles)(
    ({classes, title, record}) => (
        <div className={classes.title}>
            {title} {record && record.name && `${record.name}`}
        </div>
    )
);

const PropertyListFilter = props => (
    <Filter {...props}>
        <SearchInput source='q' alwaysOn />
    </Filter>
);

export const PropertyList = withStyles(styles)(
    ({ classes, ...props }) => (
        <List  
            title={<PropertyTitle title='EISS™Cubes Property' />}
            filters={<PropertyListFilter />}
            sort={{ field: 'name', order: 'ASC' }}
            perPage={10}
            exporter={exporter}
            {...props}
        >
            <Responsive
                small={
                    <SimpleList
                        linkType="edit"
                        primaryText={record => record.label}
                        secondaryText={record => record.name}
                        tertiaryText={record => record.description}
                    />
                }
                medium={
                    <Datagrid classes={{ rowEven: classes.rowEven }} >
                        <TextField source='name' label='Name' />
                        <TextField source='label' label='Label' />
                        <TextField source='description' label='Description' />
                        <EditButton />
                    </Datagrid>
                }
            />
        </List>
    )
);

const validateValue = [required(), maxLength(20)];

export const PropertyEdit = withStyles(styles)(
    ({ classes, ...props }) => (
        <Edit  
            title={<PropertyTitle title='Edit EISS™Cube Property -' />}
            {...props}
        >
            <SimpleForm redirect='list'>
                <TextInput label='Name' source='name' validate={validateValue} formClassName={classes.inline} />
                <TextInput label='Label' source='label' validate={validateValue} formClassName={classes.inline} />
                <TextInput label='Description' source='description' validate={[maxLength(50)]} className={classes.longText} />
            </SimpleForm>
        </Edit>
    )
);

export const PropertyCreate = withStyles(styles)(
    ({ classes, ...props }) => (
        <Create 
            title={<PropertyTitle title='Create a new EISS™Cube Property' />} 
            {...props}
        >
            <SimpleForm redirect='list'>
                <TextInput label='Name' source='name' validate={validateValue} formClassName={classes.inline} />
                <TextInput label='Label' source='label' validate={validateValue} formClassName={classes.inline} />
                <TextInput label='Description' source='description' validate={[maxLength(50)]} className={classes.longText} />
            </SimpleForm>
        </Create>
    )
);
