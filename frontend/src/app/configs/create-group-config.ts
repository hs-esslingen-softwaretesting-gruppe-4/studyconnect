import { FormConfig } from './../components/form-component/form-component.component';
export const CREATE_GROUP_CONFIG : FormConfig = {
  title: 'Create New Group',
  fields: [
    {
      label: 'Group Name',
      name: 'name',
      type: 'text',
      placeholder: 'Enter group name',
      required: true,
    },
    {
      label: 'Description',
      name: 'description',
      type: 'textarea',
      placeholder: 'Enter group description',
      required: false,
    },
    {
      label: 'Visibility',
      name: 'is_public',
      type: 'select',
      placeholder: 'Visibility of the group',
      required: true,
      options: [
        { label: 'Public', value: true },
        { label: 'Private', value: false },
      ],
    },
    {
      label: 'Maximum Members',
      name: 'max_members',
      type: 'number',
      placeholder: 'Enter maximum number of members',
      required: false
    },
    {
      label: 'Members',
      name: 'member_ids',
      type: 'autocomplete',
      placeholder: 'Add members to the group',
      required: true,
      emitAsSignal: true,
    },
    {
      label: 'Admins',
      name: 'admin_ids',
      type: 'autocomplete',
      placeholder: 'Add admins to the group',
      required: true,
      emitAsSignal: true,
    },
  ],
}
