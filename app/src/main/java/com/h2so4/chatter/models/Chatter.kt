package com.h2so4.chatter.models

import android.os.Parcel
import android.os.Parcelable

data class Chatter(
    var fullName: String?,
    var username: String?,
    var email: String?,
    var phoneNumber: String?,
    var password: String?,
    var birth: String?,
    var gender: String?,
    var profilePicture: String?,
    var token: String?
): Parcelable {


    constructor(parcel: Parcel) : this(
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString()
    ) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(fullName)
        parcel.writeString(username)
        parcel.writeString(email)
        parcel.writeString(phoneNumber)
        parcel.writeString(password)
        parcel.writeString(birth)
        parcel.writeString(gender)
        parcel.writeString(profilePicture)
        parcel.writeString(token)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Chatter> {
        override fun createFromParcel(parcel: Parcel): Chatter {
            return Chatter(parcel)
        }

        override fun newArray(size: Int): Array<Chatter?> {
            return arrayOfNulls(size)
        }
    }

}